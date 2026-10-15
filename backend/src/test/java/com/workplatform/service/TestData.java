package com.workplatform.service;

import com.workplatform.entity.*;
import com.workplatform.entity.Enums.*;

/** Small builders for entity graphs used by the unit tests. */
final class TestData {
    private TestData() {}

    static User user(long id, String name) {
        User u = new User();
        u.setId(id); u.setName(name); u.setEmail(name.toLowerCase() + "@example.com"); u.setPasswordHash("hash");
        return u;
    }

    static Organization org(long id) {
        Organization o = new Organization();
        o.setId(id); o.setName("Org " + id);
        return o;
    }

    static Workspace workspace(long id, Organization org) {
        Workspace w = new Workspace();
        w.setId(id); w.setName("WS " + id); w.setOrganization(org);
        return w;
    }

    static Project project(long id, Workspace ws) {
        Project p = new Project();
        p.setId(id); p.setName("Project " + id); p.setWorkspace(ws);
        return p;
    }

    static Status status(long id, Workspace ws, StatusCategory category, String name) {
        Status s = new Status();
        s.setId(id); s.setName(name); s.setCategory(category); s.setWorkspace(ws);
        return s;
    }

    static Priority priority(long id, Workspace ws, int level, String name) {
        Priority p = new Priority();
        p.setId(id); p.setName(name); p.setLevel(level); p.setWorkspace(ws);
        return p;
    }

    static Task task(long id, Project project, Status status, Priority priority, User creator) {
        Task t = new Task();
        t.setId(id); t.setTitle("Task " + id); t.setProject(project); t.setStatus(status); t.setPriority(priority); t.setCreatedBy(creator);
        return t;
    }

    static Membership membership(User u, Organization o, OrgRole role) {
        Membership m = new Membership();
        m.setUser(u); m.setOrganization(o); m.setRole(role);
        return m;
    }
}
