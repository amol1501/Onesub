package com.onesub.merchant.repository;

import com.onesub.merchant.entity.Merchant;
import com.onesub.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    List<Merchant> findByOwnerIsNullOrOwner(User owner);
    Page<Merchant> findByOwnerIsNullOrOwner(User owner, Pageable pageable);
    Optional<Merchant> findByIdAndOwnerIsNull(Long id);
    Optional<Merchant> findByIdAndOwner(Long id, User owner);
    boolean existsByOwnerAndNameIgnoreCase(User owner, String name);
    boolean existsByOwnerIsNullAndNameIgnoreCase(String name);
}
