package HighThroughPutExchange.API;

import HighThroughPutExchange.API.api_objects.*;
import HighThroughPutExchange.API.database_objects.Session;
import HighThroughPutExchange.API.database_objects.User;
import HighThroughPutExchange.Database.entry.DBEntry;
import HighThroughPutExchange.Database.exceptions.AlreadyExistsException;
import HighThroughPutExchange.Database.exceptions.NotFoundException;
import HighThroughPutExchange.Database.localdb.LocalDBClient;
import HighThroughPutExchange.Database.localdb.LocalDBTable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashMap;

/*
todo
    build token generator
    make abstract payloads for admin vs private vs public pages for streamlined authentication
    make all DBs threadsafe
        Atomicity - not guaranteed, but expose backing and mutex
        Consistency - guaranteed
        Isolation - guaranteed
        Durability - guaranteed
 */

@SpringBootApplication
@RestController
public class ServerApplication {

    private final String adminUsername = "trading_club_admin";
    private final String adminPassword = "abcxyz";

    private LocalDBClient dbClient;
    private LocalDBTable<User> users;
    private LocalDBTable<Session> sessions;

    private static final int KEY_LENGTH = 16;

    private static char randomChar() {
        return (char) ((int) (Math.random() * 26 + 65));
    }

    private static String generateKey() {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < KEY_LENGTH; ++i) {
            output.append(randomChar());
        }
        return output.toString();
    }

    public ServerApplication() {
        HashMap<String, Class<? extends DBEntry>> mapping = new HashMap<>();
        mapping.put("users", User.class);
        mapping.put("sessions", Session.class);
        dbClient = new LocalDBClient("data.json", mapping);
        try {
            users = dbClient.getTable("users");
        } catch (NotFoundException e) {
            try {
                dbClient.createTable("users");
                users = dbClient.getTable("users");
            } catch (AlreadyExistsException ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            sessions = dbClient.getTable("sessions");
        } catch (NotFoundException e) {
            try {
                dbClient.createTable("sessions");
                sessions = dbClient.getTable("sessions");
            } catch (AlreadyExistsException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    // public pages

    @GetMapping("/")
    public String home() {
        return "Hello, World! This is a public page.";
    }

    // admin pages

    @PostMapping("/add_user")
    public AddUserResponse addUser(@RequestBody AddUserRequest form) {
        if (form.getAdminUsername().equals(adminUsername) && form.getAdminPassword().equals(adminPassword)) {
            if (users.containsItem(form.getUsername())) {
                return new AddUserResponse(true, false, "username already exists");
            }
            try {
                users.putItem(new User(form.getUsername(), form.getName(), generateKey(), form.getEmail()));
            } catch (AlreadyExistsException e) {
                throw new RuntimeException(e);
            }
            return new AddUserResponse(true, true, "user successfully created with key " + users.getItem(form.getUsername()).getApiKey());
        }
        return new AddUserResponse(false, false, "incorrect username or password");
    }

    @PostMapping("/admin_page")
    public AdminDashboardResponse adminPage(@RequestBody AdminDashboardRequest form) {
        if (form.getAdminUsername().equals(adminUsername) && form.getAdminPassword().equals(adminPassword)) {
                return new AdminDashboardResponse(true, false, "this is the admin dashboard");
        }
        return new AdminDashboardResponse(false, false, "failed authentication");
    }

    @PostMapping("/shutdown")
    public ShutdownResponse shutdown(@RequestBody ShutdownRequest form) {
        if (form.getAdminUsername().equals(adminUsername) && form.getAdminPassword().equals(adminPassword)) {
            return new ShutdownResponse(false, false);
        }
        try {
            dbClient.closeClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new ShutdownResponse(true, true);
    }

    // private pages

    @PostMapping("/buildup")
    public BuildupResponse buildup(@RequestBody BuildupRequest form) {
        // if username not found
        if (!users.containsItem(form.getUsername())) {
            return new BuildupResponse(false, false, "");
        }

        User u = users.getItem(form.getUsername());
        // if username and api key mismatch
        if (!u.getApiKey().equals(form.getApiKey())) {
            return new BuildupResponse(false, false, "");
        }

        Session s = new Session(generateKey(), u.getUsername());
        try {
            sessions.putItem(s);
        } catch (AlreadyExistsException e) {
            throw new RuntimeException(e);
        }

        return new BuildupResponse(true, true, s.getSessionToken());
    }

    @PostMapping("/teardown")
    public TeardownResponse teardown(@RequestBody TeardownRequest form) {
        // if username not found
        if (!sessions.containsItem(form.getUsername())) {
            return new TeardownResponse(false, false);
        }

        Session s = sessions.getItem(form.getUsername());
        // if username and api key mismatch
        if (!s.getSessionToken().equals(form.getSessionToken())) {
            return new TeardownResponse(false, false);
        }

        return new TeardownResponse(true, true);
    }

    @PostMapping("/privatePage")
    public PrivatePageResponse privatePage(@RequestBody PrivatePageRequest form) {
        if (!sessions.containsItem(form.getUsername())) {
            return new PrivatePageResponse(false, false, "");
        }

        Session s = sessions.getItem(form.getUsername());
        if (!s.getSessionToken().equals(form.getSessionToken())) {
            return new PrivatePageResponse(false, false, "");
        }

        return new PrivatePageResponse(true, true, "this is a private page");
    }
}
