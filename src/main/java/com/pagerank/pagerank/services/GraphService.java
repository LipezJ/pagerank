package com.pagerank.pagerank.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import com.pagerank.pagerank.domain.model.Follow;
import com.pagerank.pagerank.domain.model.Person;
import com.pagerank.pagerank.domain.model.Rank;
import com.pagerank.pagerank.domain.model.RankDelta;
import com.pagerank.pagerank.domain.repository.FollowRepository;
import com.pagerank.pagerank.domain.repository.PersonRepository;
import com.pagerank.pagerank.domain.repository.RankDeltaRepository;
import com.pagerank.pagerank.domain.repository.RankRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class GraphService {

	private final PersonRepository personRepository;
	private final FollowRepository followRepository;
	private final RankRepository rankRepository;
	private final RankDeltaRepository rankDeltaRepository;

	public GraphService(
			PersonRepository personRepository,
			FollowRepository followRepository,
			RankRepository rankRepository,
			RankDeltaRepository rankDeltaRepository) {
		this.personRepository = personRepository;
		this.followRepository = followRepository;
		this.rankRepository = rankRepository;
		this.rankDeltaRepository = rankDeltaRepository;
	}

	/**
	 * Crea una persona con la fecha actual.
	 *
	 * @param name nombre normalizado de la persona.
	 * @param spamScore puntaje de spam entre 0 y 1.
	 * @return persona persistida.
	 */
	public Person registerPerson(String name, double spamScore) {
		return registerPerson(name, spamScore, Instant.now());
	}

	/**
	 * Crea una persona con un lastSeen especifico.
	 *
	 * @param name nombre normalizado de la persona.
	 * @param spamScore puntaje de spam entre 0 y 1.
	 * @param lastSeen instante de la ultima observacion; si es nulo se usa ahora.
	 * @return persona persistida.
	 */
	public Person registerPerson(String name, double spamScore, Instant lastSeen) {
		Instant effectiveLastSeen = lastSeen != null ? lastSeen : Instant.now();
		Person person = new Person(name, spamScore, effectiveLastSeen);
		return personRepository.save(person);
	}

	/**
	 * Obtiene una persona por id o lanza excepcion si no existe.
	 *
	 * @param personId id de la persona.
	 * @return persona existente.
	 */
	@Transactional(readOnly = true)
	public Person getPerson(Long personId) {
		return requirePerson(personId);
	}

	/**
	 * Actualiza el lastSeen de una persona.
	 *
	 * @param personId id de la persona.
	 * @param lastSeen instante observado; si es nulo se usa ahora.
	 * @return persona actualizada.
	 */
	public Person touchPerson(Long personId, Instant lastSeen) {
		Person person = requirePerson(personId);
		person.setLastSeen(lastSeen != null ? lastSeen : Instant.now());
		return personRepository.save(person);
	}

	/**
	 * Crea o actualiza un follow dirigido entre dos personas.
	 *
	 * @param sourceId id origen.
	 * @param targetId id destino.
	 * @param quality calidad/weight de la arista.
	 * @param lastSeen instante observado; si es nulo se usa ahora.
	 * @return follow persistido.
	 */
	public Follow registerFollow(Long sourceId, Long targetId, double quality, Instant lastSeen) {
		Person source = requirePerson(sourceId);
		Person target = requirePerson(targetId);
		Instant effectiveLastSeen = lastSeen != null ? lastSeen : Instant.now();

		return followRepository.findBySourceIdAndTargetId(sourceId, targetId)
				.map(existing -> updateFollow(existing, source, target, quality, effectiveLastSeen))
				.orElseGet(() -> followRepository.save(new Follow(source, target, quality, effectiveLastSeen)));
	}

	/**
	 * Lista todas las personas ordenadas alfabeticamente.
	 *
	 * @return personas ordenadas.
	 */
	@Transactional(readOnly = true)
	public List<Person> getAllPersonsSorted() {
		return personRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
	}

	private Follow updateFollow(Follow follow, Person source, Person target, double quality, Instant lastSeen) {
		follow.setSource(source);
		follow.setTarget(target);
		follow.setQuality(quality);
		follow.setLastSeen(lastSeen);
		return followRepository.save(follow);
	}

	/**
	 * Crea o actualiza el rank de una persona con la fecha actual.
	 *
	 * @param personId id de la persona.
	 * @param score valor de PageRank calculado.
	 * @return rank persistido.
	 */
	public Rank upsertRank(Long personId, double score) {
		Person person = requirePerson(personId);
		Instant now = Instant.now();
		Rank rank = rankRepository.findByPerson(person)
				.orElse(new Rank(person, score, now));
		rank.setPerson(person);
		rank.setScore(score);
		rank.setUpdatedAt(now);
		return rankRepository.save(rank);
	}

	/**
	 * Crea o actualiza el delta de rank de una persona.
	 *
	 * @param personId id de la persona.
	 * @param delta diferencia respecto al score previo.
	 * @return RankDelta persistido.
	 */
	public RankDelta upsertRankDelta(Long personId, double delta) {
		Person person = requirePerson(personId);
		RankDelta rankDelta = rankDeltaRepository.findByPerson(person)
				.orElse(new RankDelta(person, delta));
		rankDelta.setPerson(person);
		rankDelta.setDelta(delta);
		return rankDeltaRepository.save(rankDelta);
	}

	/**
	 * Devuelve los follows salientes de una persona.
	 *
	 * @param personId id de la persona origen.
	 * @return lista de follows salientes.
	 */
	@Transactional(readOnly = true)
	public List<Follow> getOutgoingFollows(Long personId) {
		return followRepository.findBySourceId(personId);
	}

	/**
	 * Devuelve los follows entrantes a una persona.
	 *
	 * @param personId id de la persona destino.
	 * @return lista de follows entrantes.
	 */
	@Transactional(readOnly = true)
	public List<Follow> getIncomingFollows(Long personId) {
		return followRepository.findByTargetId(personId);
	}

	/**
	 * Elimina follows salientes que no esten en el conjunto a conservar y devuelve ids tocados.
	 *
	 * @param personId id del origen cuyos follows se sincronizan.
	 * @param keepTargetIds ids de destino a conservar; si es nulo se borran todos.
	 * @return ids de personas afectadas (origen y destinos eliminados).
	 */
	public Set<Long> syncOutgoingFollows(Long personId, Set<Long> keepTargetIds) {
		Person person = requirePerson(personId);
		List<Follow> outgoing = followRepository.findBySourceId(personId);

		Set<Long> keepTargets = keepTargetIds != null ? new HashSet<>(keepTargetIds) : Set.of();
		List<Follow> toDelete = new ArrayList<>();
		for (Follow follow : outgoing) {
			if (!keepTargets.contains(follow.getTarget().getId())) {
				toDelete.add(follow);
			}
		}
		if (!toDelete.isEmpty()) {
			followRepository.deleteAll(toDelete);
		}

		Set<Long> touched = new HashSet<>();
		touched.add(person.getId());
		for (Follow follow : toDelete) {
			touched.add(follow.getTarget().getId());
		}
		return touched;
	}

	private Person requirePerson(Long personId) {
		return personRepository.findById(personId)
				.orElseThrow(() -> new EntityNotFoundException("Person " + personId + " not found"));
	}
}
