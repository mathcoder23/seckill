package com.github.lyrric.service;

import com.github.lyrric.conf.Config;
import com.github.lyrric.model.BusinessException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketTimeoutException;

/**
 * @author wangxiaodong
 */
public class SecKillRunnable implements Runnable {

    private final Logger logger = LogManager.getLogger(SecKillService.class);
    /**
     * 是否刷新st
     */
    private boolean resetSt;
    /**
     * httpService
     */
    private HttpService httpService;
    /**
     * 疫苗id
     */
    private Integer vaccineId;
    /**
     * 开始时间
     */
    private long startDate;

    /**
     * 请求次数
     */
    private int reqCount;

    /**
     * 提前时间
     */
    private int earlyTime;

    /**
     * st刷新阀值
     */
    private int stRefreshTh;

    private boolean daemon;

    public SecKillRunnable(boolean resetSt, HttpService httpService, Integer vaccineId, long startDate, int earlyTime, int stRefreshTh, boolean daemon) {
        this.resetSt = resetSt;
        this.httpService = httpService;
        this.vaccineId = vaccineId;
        this.startDate = startDate;
        this.earlyTime = earlyTime;
        this.stRefreshTh = stRefreshTh;
        this.daemon = daemon;
        this.reqCount = 0;
    }

    @Override
    public void run() {
        do {
            if (Config.success != null && Config.success) {
                return;
            }
            if (System.currentTimeMillis() + earlyTime < startDate) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            long id = Thread.currentThread().getId();
            try {
                //获取加密参数st
                if (resetSt) {
                    logger.info("Thread ID：{}，请求获取加密参数st", id);
                    Config.st = httpService.getSt(vaccineId.toString());
                    logger.info("Thread ID：{}，成功获取加密参数st", id);
                }
                logger.info("Thread ID：{}，秒杀请求", id);
                httpService.secKill(vaccineId.toString(), "1", Config.memberId.toString(),
                        Config.idCard, Config.st);
                Config.success = true;
                for (int i = 0; i < 100; i++) {
                    logger.error("Thread ID：{}，抢购成功", id);
                }
                return;
            } catch (BusinessException e) {
                logger.info("Thread ID: {}, 抢购失败: {}", id, e.getErrMsg());
//                if (e.getErrMsg().contains("没抢到")) {
//                    Config.success = false;
//                    break;
//                }
            } catch (ConnectTimeoutException | SocketTimeoutException socketTimeoutException) {
                logger.error("Thread ID: {},抢购失败: 超时了", Thread.currentThread().getId());
            } catch (Exception e) {
                logger.warn("Thread ID: {}，未知异常", Thread.currentThread().getId());
            } finally {
                reqCount++;
                //如果离开始时间2分钟后，或者已经成功抢到则不再继续
                if (System.currentTimeMillis() > startDate + 1000 * 90) {
                    break;
                }
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        } while (true);
    }
}
