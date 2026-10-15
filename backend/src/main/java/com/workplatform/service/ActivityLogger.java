package com.workplatform.service;

import com.workplatform.entity.*;
import com.workplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityLogger {
    private final ActivityRepository activities;
    private final UserRepository users;

    public void log(Project project, Task task, Long actorId, String type, String message) {
        Activity a = new Activity();
        a.setProject(project); a.setTask(task); a.setType(type); a.setMessage(message);
        a.setActor(users.getReferenceById(actorId));
        activities.save(a);
    }
}
