package com.workplatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end flow with two real users in one team and a third user in a different organization. */
@SpringBootTest(properties = "app.scheduling.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class MultiUserWorkflowIntegrationTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry r) throws Exception {
        r.add("app.storage.dir", () -> {
            try { return Files.createTempDirectory("uploads").toString(); } catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode send(MockHttpServletRequestBuilder b, String token, Object body, int expected) throws Exception {
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        MvcResult res = mvc.perform(b).andExpect(status().is(expected)).andReturn();
        String content = res.getResponse().getContentAsString();
        return content.isBlank() ? null : json.readTree(content);
    }

    private JsonNode get(String url, String token) throws Exception { return send(get(url), token, null, 200); }
    private JsonNode post(String url, String token, Object body) throws Exception { return send(post(url), token, body, 200); }
    private JsonNode patch(String url, String token, Object body) throws Exception { return send(patch(url), token, body, 200); }

    private JsonNode register(String name, String email, String org, String inviteToken) throws Exception {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", name); body.put("email", email); body.put("password", "password123");
        if (org != null) body.put("organizationName", org);
        if (inviteToken != null) body.put("inviteToken", inviteToken);
        return post("/api/auth/register", null, body);
    }

    @Test
    void teamCollaborationEndToEnd() throws Exception {
        // --- Alice signs up: gets her own organization, workspace, and default statuses/priorities
        JsonNode a = register("Alice", "alice@example.com", "Acme", null);
        String alice = a.get("token").asText();
        long aliceId = a.get("user").get("id").asLong();
        long ws = get("/api/workspaces", alice).get(0).get("id").asLong();
        JsonNode meta = get("/api/workspaces/" + ws + "/meta", alice);
        assertEquals(4, meta.get("statuses").size());
        assertEquals(4, meta.get("priorities").size());
        long todo = meta.get("statuses").get(0).get("id").asLong();
        long done = meta.get("statuses").get(3).get("id").asLong();
        long high = meta.get("priorities").get(2).get("id").asLong();

        // duplicate sign-up is rejected, login works
        send(post("/api/auth/register"), null, Map.of("name", "A2", "email", "ALICE@example.com", "password", "password123"), 409);
        post("/api/auth/login", null, Map.of("email", "alice@example.com", "password", "password123"));
        send(post("/api/auth/login"), null, Map.of("email", "alice@example.com", "password", "wrong-password"), 401);

        // --- project, epic, custom field, task
        long project = post("/api/workspaces/" + ws + "/projects", alice, Map.of("name", "Launch")).get("id").asLong();
        long epic = post("/api/projects/" + project + "/epics", alice, Map.of("name", "Marketing")).get("id").asLong();
        long budget = post("/api/workspaces/" + ws + "/custom-fields", alice, Map.of("name", "Budget", "type", "NUMBER")).get("id").asLong();

        JsonNode task = post("/api/projects/" + project + "/tasks", alice, Map.of(
                "title", "Write plan", "priorityId", high, "assigneeId", aliceId, "epicId", epic,
                "tags", List.of("planning"), "customFields", Map.of(budget, "1500")));
        long taskId = task.get("id").asLong();
        assertEquals("To do", task.get("statusName").asText());
        assertEquals("High", task.get("priorityName").asText());
        assertEquals("1500", task.get("customFields").get(String.valueOf(budget)).asText());
        send(post("/api/projects/" + project + "/tasks"), alice, Map.of("title", "Bad", "customFields", Map.of(budget, "lots")), 400);

        // --- Alice invites Bob (no account yet) -> link; Bob signs up through it and lands in Alice's workspace
        JsonNode invite = post("/api/workspaces/" + ws + "/invitations", alice, Map.of("email", "bob@example.com"));
        assertTrue(invite.get("invited").asBoolean());
        String inviteToken = invite.get("invitation").get("token").asText();
        assertEquals("bob@example.com", send(get("/api/invitations/" + inviteToken), null, null, 200).get("email").asText());

        send(post("/api/auth/register"), null, Map.of("name", "Mallory", "email", "mallory@example.com", "password", "password123", "inviteToken", inviteToken), 400);
        JsonNode b = register("Bob", "bob@example.com", null, inviteToken);
        String bob = b.get("token").asText();
        long bobId = b.get("user").get("id").asLong();
        assertEquals(ws, get("/api/workspaces", bob).get(0).get("id").asLong());
        assertEquals(2, get("/api/workspaces/" + ws + "/members", alice).size());
        send(get("/api/invitations/" + inviteToken), null, null, 400);   // single use

        // --- assignment notifies Bob; Bob completes it
        patch("/api/tasks/" + taskId, alice, Map.of("assigneeId", bobId));
        assertEquals(1, get("/api/notifications/unread-count", bob).get("count").asLong());
        patch("/api/tasks/" + taskId, bob, Map.of("statusId", done));
        JsonNode finished = get("/api/tasks/" + taskId, bob);
        assertEquals("DONE", finished.get("category").asText());
        assertFalse(finished.get("completedAt").isNull());
        assertTrue(get("/api/tasks/" + taskId + "/activity", bob).size() >= 3);

        // --- members can't change workspace configuration
        send(post("/api/workspaces/" + ws + "/statuses"), bob, Map.of("name", "Blocked", "category", "IN_PROGRESS"), 403);
        post("/api/workspaces/" + ws + "/statuses", alice, Map.of("name", "Blocked", "category", "IN_PROGRESS"));
        assertEquals(5, get("/api/workspaces/" + ws + "/statuses", bob).size());

        // --- subtasks (one level only)
        long sub = post("/api/tasks/" + taskId + "/subtasks", alice, Map.of("title", "Draft outline")).get("id").asLong();
        assertEquals(1, get("/api/tasks/" + taskId, alice).get("subtaskTotal").asInt());
        send(post("/api/tasks/" + sub + "/subtasks"), alice, Map.of("title", "Too deep"), 400);

        // --- recurring task: completing it schedules the next one
        JsonNode weekly = post("/api/projects/" + project + "/tasks", alice, Map.of(
                "title", "Weekly report", "recurrence", "WEEKLY", "dueDate", LocalDate.now().plusDays(1).toString()));
        patch("/api/tasks/" + weekly.get("id").asLong(), alice, Map.of("statusId", done));
        JsonNode found = get("/api/workspaces/" + ws + "/tasks?q=weekly", alice);
        assertEquals(2, found.size());
        assertEquals(1, get("/api/workspaces/" + ws + "/tasks?q=weekly&category=TODO", alice).size());

        // --- filtering
        assertEquals(1, get("/api/workspaces/" + ws + "/tasks?assignee=me&includeSubtasks=false&tag=planning", bob).size());
        assertEquals(0, get("/api/workspaces/" + ws + "/tasks?assignee=none&tag=planning", alice).size());

        // --- daily planning
        patch("/api/tasks/" + weekly.get("id").asLong(), alice, Map.of("assigneeId", aliceId, "plannedDate", LocalDate.now().minusDays(1).toString()));
        // (completed tasks are not carried over)
        assertEquals(0, post("/api/workspaces/" + ws + "/my-day/carry-forward", alice, Map.of()).get("moved").asInt());

        // --- attachments
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello team".getBytes());
        MvcResult up = mvc.perform(multipart("/api/tasks/" + taskId + "/attachments").file(file).header("Authorization", "Bearer " + bob))
                .andExpect(status().isOk()).andReturn();
        long attachmentId = json.readTree(up.getResponse().getContentAsString()).get("id").asLong();
        MvcResult dl = mvc.perform(get("/api/attachments/" + attachmentId + "/download").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk()).andReturn();
        assertEquals("hello team", dl.getResponse().getContentAsString());
        assertTrue(dl.getResponse().getHeader("Content-Disposition").startsWith("attachment"));

        // --- dashboard / reports / export
        JsonNode dash = get("/api/workspaces/" + ws + "/dashboard", alice);
        assertEquals(5, dash.get("statusCounts").size());
        assertTrue(get("/api/workspaces/" + ws + "/reports", alice).get("completed").asInt() >= 1);
        MvcResult csv = mvc.perform(get("/api/workspaces/" + ws + "/tasks/export.csv").header("Authorization", "Bearer " + alice)).andExpect(status().isOk()).andReturn();
        assertTrue(csv.getResponse().getContentAsString().contains("Write plan"));

        // --- a user from another organization sees nothing
        String carol = register("Carol", "carol@example.com", "Other Co", null).get("token").asText();
        send(get("/api/projects/" + project), carol, null, 403);
        send(get("/api/tasks/" + taskId), carol, null, 403);
        send(get("/api/workspaces/" + ws + "/dashboard"), carol, null, 403);
        send(get("/api/attachments/" + attachmentId + "/download"), carol, null, 403);
        assertEquals(1, get("/api/workspaces", carol).size());

        // --- unauthenticated and bad tokens
        mvc.perform(get("/api/workspaces")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/workspaces").header("Authorization", "Bearer garbage")).andExpect(status().isUnauthorized());

        // --- removing Bob unassigns his work and cuts off access
        mvc.perform(delete("/api/workspaces/" + ws + "/members/" + bobId).header("Authorization", "Bearer " + alice)).andExpect(status().isOk());
        send(get("/api/tasks/" + taskId), bob, null, 403);
        assertTrue(get("/api/tasks/" + taskId, alice).get("assignee").isNull());
    }
}
