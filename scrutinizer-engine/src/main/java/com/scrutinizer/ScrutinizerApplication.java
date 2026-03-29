package com.scrutinizer;

import com.scrutinizer.cli.ScrutinizerCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class ScrutinizerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ScrutinizerApplication.class);
        app.setLogStartupInfo(false);
        System.exit(SpringApplication.exit(app.run(args)));
    }

    @Bean
    @Profile("cli")
    public CommandLineRunner commandLineRunner(ScrutinizerCommand command) {
        return command::run;
    }
}
