package com.selectcode.sms.iam.infra;

import com.selectcode.sms.iam.domain.AppUser;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Tenant-scoped: every query is limited to the current school by Hibernate and by row-level security. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	@Query("select u from AppUser u where lower(u.username) = lower(?1)")
	Optional<AppUser> findByUsernameIgnoreCase(String username);

	@Query("""
			select u from AppUser u
			where (:q = '' or lower(u.username) like concat('%', :q, '%')
			       or lower(u.displayName) like concat('%', :q, '%')
			       or lower(coalesce(u.email, '')) like concat('%', :q, '%'))
			  and u.status in :statuses
			order by u.displayName""")
	Page<AppUser> search(String q, java.util.Collection<AppUser.Status> statuses, Pageable pageable);

	Optional<AppUser> findByMainAdminTrue();

	Optional<AppUser> findByStaffId(long staffId);

	long countByStatus(AppUser.Status status);
}
