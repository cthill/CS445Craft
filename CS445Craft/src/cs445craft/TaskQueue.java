/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs445craft;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author cthill
 */
public class TaskQueue {
    private Queue<Runnable> queue;
    
    
    public TaskQueue() {
        queue = new LinkedList<>();
    }
    
    public void run(int n) {
        for (int i = 0; i < n; i++) {
            Runnable task = queue.poll();
            if (task == null) {
                break;
            }
            
            task.run();
        }
    }
    
    public void addTask(Runnable task) {
        queue.add(task);
    }
    
    public void addTasks(Collection<Runnable> tasks) {
        queue.addAll(tasks);
    }
}
