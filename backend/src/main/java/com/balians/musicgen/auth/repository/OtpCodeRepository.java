package com.balians.musicgen.auth.repository;

import com.balians.musicgen.auth.model.OtpCode;
import com.balians.musicgen.auth.model.OtpPurpose;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OtpCodeRepository extends MongoRepository<OtpCode, String> {

    Optional<OtpCode> findFirstByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    List<OtpCode> findByEmailAndPurposeAndConsumedAtIsNull(String email, OtpPurpose purpose);

    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);
}
