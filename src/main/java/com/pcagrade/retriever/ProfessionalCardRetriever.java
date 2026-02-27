package com.pcagrade.retriever;

import com.pcagrade.mason.jpa.repository.EnableMasonRevisionRepositories;
import com.pcagrade.mason.jpa.revision.RevisionInfo;
import com.pcagrade.painer.client.EnablePainter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnablePainter
@EnableCaching
@EnableMasonRevisionRepositories
@EntityScan(basePackageClasses = {ProfessionalCardRetriever.class, RevisionInfo.class})
@EnableAsync
public class ProfessionalCardRetriever {

	public static void main(String[] args) {
		SpringApplication.run(ProfessionalCardRetriever.class, args);
	}
}
