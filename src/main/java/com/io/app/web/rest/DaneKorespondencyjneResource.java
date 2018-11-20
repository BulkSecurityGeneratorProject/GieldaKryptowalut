package com.io.app.web.rest;


import com.codahale.metrics.annotation.Timed;
import com.io.app.domain.DaneKorespondencyjne;
import com.io.app.service.DaneKorespondencyjneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DaneKorespondencyjneResource {
    private final Logger log = LoggerFactory.getLogger(DaneKorespondencyjneResource.class);

    private final DaneKorespondencyjneService daneKorespondencyjneService;

    public DaneKorespondencyjneResource(DaneKorespondencyjneService daneKorespondencyjneService) {
        this.daneKorespondencyjneService = daneKorespondencyjneService;
    }

    @GetMapping("/daneKorespondencyjne")
    @Timed
    public ResponseEntity<List<DaneKorespondencyjne>> findAll(){
        return new ResponseEntity<List<DaneKorespondencyjne>>(this.daneKorespondencyjneService.findAll(), HttpStatus.OK);
    }
}
