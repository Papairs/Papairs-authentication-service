package com.papairs.docs.repository;

import com.papairs.docs.model.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFileRepository extends JpaRepository<UserFile, String> {
    
    List<UserFile> findByUserId(String userId);
    
    Optional<UserFile> findByUserIdAndFilename(String userId, String filename);
    
    boolean existsByUserIdAndFilename(String userId, String filename);
}
