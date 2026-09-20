package com.civicfix.util;

import com.civicfix.repository.ComplaintRepository;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ComplaintIdGenerator {

    private final ComplaintRepository complaintRepository;
    private final AtomicLong counter = new AtomicLong(0);

    public ComplaintIdGenerator(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    public synchronized String generateId() {
        if (counter.get() == 0) {
            long total = complaintRepository.count();
            counter.set(total);
        }
        long nextVal = counter.incrementAndGet();
        int currentYear = Year.now().getValue();
        return String.format("CIV-%d-%05d", currentYear, nextVal);
    }
}
