package com.pagerank.pagerank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI pageRankOpenApi() {
		return new OpenAPI()
				.components(new Components())
				.info(new Info()
						.title("PageRank API")
						.description("API REST mínima para gestionar el grafo y ejecutar PageRank.")
						.version("0.1.0")
						.contact(new Contact().name("Equipo PageRank")));
	}
}
