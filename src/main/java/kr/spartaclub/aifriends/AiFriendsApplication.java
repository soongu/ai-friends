package kr.spartaclub.aifriends;

import kr.spartaclub.aifriends.config.DotenvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiFriendsApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AiFriendsApplication.class);
        app.addInitializers(new DotenvInitializer());
        app.run(args);
    }
}
