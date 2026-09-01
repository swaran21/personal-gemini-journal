package com.pm.personalgeminijournalbackend.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagChatRequestTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void acceptsPublicUserModelTextHistoryShape() throws Exception {
        RagChatRequest request = mapper.readValue("""
                {"query":"What did I learn?","history":[
                  {"role":"user","text":"Tell me about Java"},
                  {"role":"model","text":"You learned generics"}
                ]}
                """, RagChatRequest.class);

        assertEquals("What did I learn?", request.question());
        assertEquals(RagChatRequest.Role.USER, request.history().get(0).role());
        assertEquals("Tell me about Java", request.history().get(0).content());
        assertEquals(RagChatRequest.Role.MODEL, request.history().get(1).role());
    }

    @Test void acceptsPriorContentAssistantShapeForCompatibility() throws Exception {
        RagChatRequest request = mapper.readValue("""
                {"question":"Follow up","history":[{"role":"ASSISTANT","content":"Earlier answer"}]}
                """, RagChatRequest.class);

        assertEquals(RagChatRequest.Role.MODEL, request.history().get(0).role());
        assertEquals("Earlier answer", request.history().get(0).content());
    }
}
