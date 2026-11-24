package com.leomall.core.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;

@Component
public class TaskService {
    private static final Log logger = LogFactory.getLog(TaskService.class);
    private TaskService taskService;
    private DelayQueue<Task> delayQueue =  new DelayQueue<Task>();

    @PostConstruct
    private void init() {
        taskService = this;

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Task task = delayQueue.take();
                        task.run();
                    } catch (Exception e) {
                        logger.error("延迟任务执行失败", e);
                    }
                }
            }
        });
    }

    public void addTask(Task task){
        if(delayQueue.contains(task)){
            return;
        }
        delayQueue.add(task);
    }

    public void removeTask(Task task){
        delayQueue.remove(task);
    }

}
