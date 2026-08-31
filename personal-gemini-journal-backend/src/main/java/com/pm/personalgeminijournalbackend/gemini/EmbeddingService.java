package com.pm.personalgeminijournalbackend.gemini;

import java.util.List;

public interface EmbeddingService {
    List<Double> embed(String text);
}
