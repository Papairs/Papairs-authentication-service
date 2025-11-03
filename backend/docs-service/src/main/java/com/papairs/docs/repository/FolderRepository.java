package com.papairs.docs.repository;

import com.papairs.docs.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {
    List<Folder> findByOwnerId(String ownerId);
    List<Folder> findByOwnerIdAndParentFolderIdIsNull(String ownerId);
    List<Folder> findByParentFolderId(String parentFolderId);
    boolean existsByParentFolderId(String parentFolderId);
    long countByParentFolderId(String parentFolderId);
}
