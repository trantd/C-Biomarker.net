package kcore.plugin.service;

import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.Task;


public class TaskFactory extends AbstractTaskFactory {
	
	private Task task;
	public TaskFactory(Task task){
		this.task = task;
	}
	
	public TaskIterator createTaskIterator()  {
		return new TaskIterator(task);
	}
}
