package com.pagerank.pagerank.services;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.pagerank.pagerank.domain.repository.PersonRepository;
import com.pagerank.pagerank.settings.PagerankSettingsProperties;

@Component
@Order(0)
public class DatasetBootstrapper implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DatasetBootstrapper.class);

	private final DatasetIngestionService ingestionService;
	private final PagerankSettingsProperties settings;
	private final PersonRepository personRepository;

	public DatasetBootstrapper(
			DatasetIngestionService ingestionService,
			PagerankSettingsProperties settings,
			PersonRepository personRepository) {
		this.ingestionService = ingestionService;
		this.settings = settings;
		this.personRepository = personRepository;
	}

	@Override
	public void run(ApplicationArguments args) {
		long count = personRepository.count();
		if (count > 0) {
			log.info("Skipping dataset bootstrap: {} persons already present", count);
			return;
		}
		Path personsPath = Path.of(settings.datasetPersons());
		Path followsPath = Path.of(settings.datasetFollows());
		log.info("Bootstrapping dataset from {} and {}", personsPath, followsPath);
		ingestionService.loadIfAvailable(personsPath, followsPath);
	}
}
