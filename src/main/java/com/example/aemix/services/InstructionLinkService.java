package com.example.aemix.services;

import com.example.aemix.dto.requests.UpdateInstructionLinkRequest;
import com.example.aemix.dto.responses.InstructionLinkResponse;
import com.example.aemix.entities.InstructionLink;
import com.example.aemix.exceptions.ResourceNotFoundException;
import com.example.aemix.repositories.InstructionLinkRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstructionLinkService {

    private final InstructionLinkRepository instructionLinkRepository;

    @PostConstruct
    @Transactional
    public void initDefaultLinks() {
        if (instructionLinkRepository.count() > 0) {
            return;
        }
        List<InstructionLink> defaults = List.of(
                InstructionLink.builder()
                        .linkKey("pinduoduo")
                        .title("PINDUODUO")
                        .subtitle("Как заказать через приложение")
                        .link("#")
                        .sortOrder(1)
                        .build(),
                InstructionLink.builder()
                        .linkKey("alipay")
                        .title("ALIPAY")
                        .subtitle("Настройка оплаты")
                        .link("#")
                        .sortOrder(2)
                        .build(),
                InstructionLink.builder()
                        .linkKey("tracking")
                        .title("ОТСЛЕЖИВАНИЕ")
                        .subtitle("Как добавить трек-номер")
                        .link("#")
                        .sortOrder(3)
                        .build()
        );
        instructionLinkRepository.saveAll(defaults);
        log.info("Initialized {} default instruction links", defaults.size());
    }

    public List<InstructionLinkResponse> getAllLinks() {
        return instructionLinkRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InstructionLinkResponse updateLink(Long id, UpdateInstructionLinkRequest request) {
        InstructionLink link = instructionLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ссылка с id " + id + " не найдена"));
        link.setLink(request.getLink());
        if (request.getSubtitle() != null) {
            link.setSubtitle(request.getSubtitle());
        }
        instructionLinkRepository.save(link);
        return toResponse(link);
    }

    private InstructionLinkResponse toResponse(InstructionLink link) {
        InstructionLinkResponse r = new InstructionLinkResponse();
        r.setId(link.getId());
        r.setLinkKey(link.getLinkKey());
        r.setTitle(link.getTitle());
        r.setSubtitle(link.getSubtitle());
        r.setLink(link.getLink());
        r.setSortOrder(link.getSortOrder());
        return r;
    }
}
