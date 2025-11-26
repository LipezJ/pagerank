package com.pagerank.pagerank.web.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pagerank.pagerank.domain.model.Follow;
import com.pagerank.pagerank.domain.model.PageRankResult;
import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.services.GraphService;
import com.pagerank.pagerank.services.PageRankService;
import com.pagerank.pagerank.settings.PagerankSettingsProperties;

import jakarta.persistence.EntityNotFoundException;

@Controller
@RequestMapping("/follows")
public class FollowViewController {

	private final GraphService graphService;
	private final PageRankService pageRankService;
	private final PagerankSettingsProperties settings;

	public FollowViewController(GraphService graphService, PageRankService pageRankService,
			PagerankSettingsProperties settings) {
		this.graphService = graphService;
		this.pageRankService = pageRankService;
		this.settings = settings;
	}

	@GetMapping("/{personId}")
	public String edit(@PathVariable Long personId, Model model) {
		Person person = loadPerson(personId);
		List<Follow> outgoing = graphService.getOutgoingFollows(personId);
		List<Person> persons = graphService.getAllPersonsSorted().stream()
				.filter(p -> !p.getId().equals(personId))
				.toList();
		model.addAttribute("person", person);
		model.addAttribute("outgoing", outgoing);
		model.addAttribute("persons", persons);
		return "follows/edit";
	}

	@PostMapping("/{personId}")
	public String update(
			@PathVariable Long personId,
			@RequestParam(value = "keepTargetId", required = false) List<Long> keepTargetIds,
			RedirectAttributes redirectAttributes) {

		Person person = loadPerson(personId);
		Set<Long> keepTargets = keepTargetIds != null ? new HashSet<>(keepTargetIds) : Set.of();
		Set<Long> touched = graphService.syncOutgoingFollows(person.getId(), keepTargets);
		PageRankResult result = pageRankService.runIncrementalUpdate(touched);
		redirectAttributes.addFlashAttribute("message", "Seguidos actualizados");
		redirectAttributes.addFlashAttribute("incremental", result);
		return "redirect:/follows/" + personId;
	}

	@PostMapping("/{personId}/add")
	public String add(
			@PathVariable Long personId,
			@RequestParam(value = "targetId", required = false) Long targetId,
			RedirectAttributes redirectAttributes) {

		if (targetId == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Debes seleccionar la persona a agregar");
			return "redirect:/follows/" + personId;
		}
		try {
			Person source = graphService.getPerson(personId);
			double adjustedQuality = adjustedQuality(source.getSpamScore());
			Follow follow = graphService.registerFollow(personId, targetId, adjustedQuality, null);
			Set<Long> touched = new HashSet<>();
			touched.add(personId);
			touched.add(targetId);
			PageRankResult result = pageRankService.runIncrementalUpdate(touched);
			redirectAttributes.addFlashAttribute("message", "Seguido agregado/actualizado: " + follow.getTarget().getName());
			redirectAttributes.addFlashAttribute("incremental", result);
		}
		catch (EntityNotFoundException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", "Persona destino no encontrada: " + targetId);
		}
		return "redirect:/follows/" + personId;
	}

	private Person loadPerson(Long personId) {
		try {
			return graphService.getPerson(personId);
		}
		catch (EntityNotFoundException ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada");
		}
	}

	private double adjustedQuality(double spamScore) {
		double clampedSpam = Math.max(0.0, Math.min(1.0, spamScore));
		double penaltyFactor = 1.0 - (settings.spamPenalty() * clampedSpam);
		if (penaltyFactor < 0) {
			penaltyFactor = 0;
		}
		return 1.0 * penaltyFactor;
	}
}
