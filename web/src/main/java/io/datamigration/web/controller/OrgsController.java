package io.datamigration.web.controller;

import io.datamigration.api.OrgsApi;
import io.datamigration.api.model.ConnectOrgRequest;
import io.datamigration.api.model.Org;
import io.datamigration.web.mapper.DtoMappers;
import io.datamigration.web.persistence.OrgConnectionEntity;
import io.datamigration.web.service.OrgService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrgsController implements OrgsApi {

    private final OrgService service;

    public OrgsController(OrgService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<Org> connectOrg(ConnectOrgRequest request) {
        OrgConnectionEntity saved =
                service.connect(
                        request.getName(),
                        request.getInstanceUrl().toString(),
                        request.getInstanceUrl().toString(),
                        request.getClientId(),
                        request.getClientSecret(),
                        request.getRefreshToken() == null ? "" : request.getRefreshToken());
        return ResponseEntity.status(201).body(DtoMappers.toDto(saved));
    }

    @Override
    public ResponseEntity<List<Org>> listOrgs() {
        return ResponseEntity.ok(service.list().stream().map(DtoMappers::toDto).toList());
    }

    @Override
    public ResponseEntity<Org> getOrg(String id) {
        return service.find(id)
                .map(DtoMappers::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
