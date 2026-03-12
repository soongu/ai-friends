package kr.spartaclub.aifriends.repository;

import kr.spartaclub.aifriends.domain.Soulmate;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Soulmate 엔티티 JPA Repository.
 */
public interface SoulmateRepository extends JpaRepository<Soulmate, Long> {
}
