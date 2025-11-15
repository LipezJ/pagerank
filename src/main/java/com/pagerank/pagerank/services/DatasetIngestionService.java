package com.pagerank.pagerank.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.domain.model.Follow;

@Service
public class DatasetIngestionService {

	private static final Logger log = LoggerFactory.getLogger(DatasetIngestionService.class);

	private final IngestionService ingestionService;

	public DatasetIngestionService(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	public void loadIfAvailable(Path personsPath, Path followsPath) {
		if (!Files.exists(personsPath) || !Files.exists(followsPath)) {
			log.info("Dataset files not found (persons: {}, follows: {}), skipping bootstrap", personsPath, followsPath);
			return;
		}

		try {
			Map<Long, Person> datasetPersons = importPersons(personsPath);
			int followCount = importFollows(followsPath, datasetPersons);
			log.info("Dataset bootstrap complete: {} persons, {} follows",
					datasetPersons.size(),
					followCount);
		}
		catch (IOException ex) {
			log.error("Failed to import dataset from {} and {}", personsPath, followsPath, ex);
		}
	}

	private Map<Long, Person> importPersons(Path path) throws IOException {
		Map<Long, Person> imported = new HashMap<>();
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			String line = reader.readLine(); // header
			if (line == null) {
				return imported;
			}
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				String[] columns = line.split(",", -1);
				if (columns.length < 4) {
					continue;
				}
				Long datasetId = parseLong(columns[0]);
				String name = columns[1].trim();
				double spam = parseDouble(columns[2], 0.0);
				Instant seen = parseInstant(columns[3]);
				if (datasetId == null || name.isBlank()) {
					continue;
				}
				Person person = ingestionService.collectPerson(new IngestionService.PersonObservation(name, spam, seen));
				imported.put(datasetId, person);
			}
		}
		log.info("Imported {} persons from {}", imported.size(), path);
		return imported;
	}

	private int importFollows(Path path, Map<Long, Person> persons) throws IOException {
		int imported = 0;
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			String line = reader.readLine(); // header
			if (line == null) {
				return imported;
			}
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				String[] columns = line.split(",", -1);
				if (columns.length < 5) {
					continue;
				}
				Long sourceDatasetId = parseLong(columns[1]);
				Long targetDatasetId = parseLong(columns[2]);
				if (sourceDatasetId == null || targetDatasetId == null) {
					continue;
				}
				Person source = persons.get(sourceDatasetId);
				Person target = persons.get(targetDatasetId);
				if (source == null || target == null) {
					continue;
				}
				double quality = parseDouble(columns[3], 0.0);
				Instant seen = parseInstant(columns[4]);
				Optional<Follow> follow = ingestionService.collectFollow(
						new IngestionService.FollowObservation(source.getId(), target.getId(), quality, seen));
				if (follow.isPresent()) {
					imported++;
				}
			}
		}
		log.info("Imported {} follows from {}", imported, path);
		return imported;
	}

	private Long parseLong(String value) {
		try {
			return Long.parseLong(value.trim());
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private double parseDouble(String value, double defaultValue) {
		try {
			return Double.parseDouble(value.trim());
		}
		catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	private Instant parseInstant(String value) {
		try {
			return Instant.parse(value.trim());
		}
		catch (Exception ex) {
			return Instant.now();
		}
	}
}
