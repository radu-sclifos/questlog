package com.radusclifos.questlog;

import com.radusclifos.questlog.shared.search.SearchRepositoryImpl;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.radusclifos.questlog", repositoryBaseClass = SearchRepositoryImpl.class)
public class QuestlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuestlogApplication.class, args);
	}

}
