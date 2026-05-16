package io.datamigration.web.service;

import io.datamigration.bulkclient.auth.InMemoryOrgTokenProvider;
import io.datamigration.bulkclient.auth.OrgCredentials;
import io.datamigration.core.domain.OrgStatus;
import io.datamigration.web.persistence.OrgConnectionEntity;
import io.datamigration.web.persistence.OrgConnectionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgService {

    private final OrgConnectionRepository repository;
    private final InMemoryOrgTokenProvider tokenProvider;

    public OrgService(
            OrgConnectionRepository repository, InMemoryOrgTokenProvider tokenProvider) {
        this.repository = repository;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public OrgConnectionEntity connect(
            String name,
            String instanceUrl,
            String loginUrl,
            String clientId,
            String clientSecret,
            String refreshToken) {
        OrgConnectionEntity entity = new OrgConnectionEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(name);
        entity.setInstanceUrl(instanceUrl);
        entity.setLoginUrl(loginUrl);
        entity.setClientId(clientId);
        entity.setClientSecret(clientSecret);
        entity.setRefreshToken(refreshToken);
        entity.setStatus(OrgStatus.CONNECTED);
        entity.setCreatedAt(Instant.now());
        OrgConnectionEntity saved = repository.save(entity);
        tokenProvider.register(
                saved.getId(),
                new OrgCredentials(loginUrl, clientId, clientSecret, refreshToken));
        return saved;
    }

    public List<OrgConnectionEntity> list() {
        return repository.findAll();
    }

    public Optional<OrgConnectionEntity> find(String id) {
        return repository.findById(id);
    }
}
