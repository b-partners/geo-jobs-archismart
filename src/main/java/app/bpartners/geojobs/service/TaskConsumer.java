package app.bpartners.geojobs.service;

import app.bpartners.geojobs.job.model.Task;
import java.util.function.Consumer;

public interface TaskConsumer<T extends Task> extends Consumer<T> {}
