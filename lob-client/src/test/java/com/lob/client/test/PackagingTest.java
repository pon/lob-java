package com.lob.client.test;

import com.google.common.collect.Iterables;
import com.lob.client.AsyncLobClient;
import com.lob.client.LobClient;
import com.lob.id.PackagingId;
import com.lob.protocol.response.CountryResponse;
import com.lob.protocol.response.CountryResponseList;
import com.lob.protocol.response.PackagingResponse;
import com.lob.protocol.response.PackagingResponseList;
import org.junit.Test;

import static com.lob.ClientUtil.print;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PackagingTest {
    private final LobClient client = AsyncLobClient.createDefault("test_0dc8d51e0acffcb1880e0f19c79b2f5b0cc");

    @Test
    public void testPackagings() throws Exception {
        final PackagingResponseList responseList = print(client.getAllPackagings().get());
        final PackagingResponse response = print(responseList.get(0));

        assertTrue(response instanceof PackagingResponse);
        assertThat(responseList.getObject(), is("list"));
        assertTrue(response.getId() instanceof PackagingId);
        assertFalse(response.getDescription().isEmpty());
        assertFalse(response.getName().isEmpty());
        assertFalse(response.getObject().isEmpty());
    }
}