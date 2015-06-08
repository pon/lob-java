package com.lob.client.test;

import com.google.common.collect.Iterables;
import com.lob.ClientUtil;
import com.lob.client.AsyncLobClient;
import com.lob.client.LobClient;
import com.lob.id.LobObjectId;
import com.lob.id.SettingId;
import com.lob.protocol.request.LobObjectRequest;
import com.lob.protocol.request.LobParam;
import com.lob.protocol.response.LobObjectDeleteResponse;
import com.lob.protocol.response.LobObjectResponse;
import com.lob.protocol.response.LobObjectResponseList;
import com.lob.protocol.response.SettingResponse;
import com.lob.protocol.response.ThumbnailResponse;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static com.lob.Util.print;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LobObjectTest {
    private final LobClient client = AsyncLobClient.createDefault("test_0dc8d51e0acffcb1880e0f19c79b2f5b0cc");

    @Test
    public void testListObjects() throws Exception {
        final LobObjectResponseList responseList = print(client.getAllLobObjects().get());
        final LobObjectResponse response = print(responseList.get(0));

        assertTrue(response instanceof LobObjectResponse);
        assertThat(responseList.getObject(), is("list"));
    }

    @Test
    public void testListObjectsLimit() throws Exception {
        final LobObjectResponseList responseList = client.getLobObjects(2).get();
        final LobObjectResponse response = Iterables.get(responseList, 0);

        assertTrue(response instanceof LobObjectResponse);
        assertThat(responseList.getCount(), is(2));

        assertThat(client.getLobObjects(1, 2).get().getCount(), is(1));
    }

    @Test(expected = ExecutionException.class)
    public void testListObjectsFail() throws Exception {
        client.getLobObjects(1000).get();
    }

    @Test
    public void testCreateObjectUrl() throws Exception {
        final LobObjectRequest.Builder builder = LobObjectRequest.builder()
            .name("Test Object")
            .file("https://s3-us-west-2.amazonaws.com/lob-assets/test.pdf")
            .setting(200);

        final LobObjectResponse response = client.createLobObject(builder.build()).get();

        assertTrue(response instanceof LobObjectResponse);
        assertThat(response.getName(), is("Test Object"));

        client.createLobObject(builder.butWith().name("other object").build()).get();

        final LobObjectDeleteResponse deleteResponse = print(client.deleteLobObject(response.getId()).get());
        assertThat(deleteResponse.getId(), is(response.getId()));

        assertFalse(response.getObject().isEmpty());
        assertTrue(response.getQuantity() > 0);
        assertTrue(response.getSetting() instanceof SettingResponse);
        assertFalse(response.getThumbnails().isEmpty());
        assertTrue(response.isDoubleSided());
        assertFalse(response.isFullBleed());
        assertFalse(response.isTemplate());

        final ThumbnailResponse thumbnail = response.getThumbnails().get(0);
        assertFalse(thumbnail.getLarge().isEmpty());
        assertFalse(thumbnail.getMedium().isEmpty());
        assertFalse(thumbnail.getSmall().isEmpty());

        final LobObjectRequest request = builder.build();
        assertFalse(request.getName().isEmpty());
        assertNull(request.isDoubleSided());
        assertNull(request.isFullBleed());
        assertNull(request.isTemplate());
        assertTrue(request.getFile() instanceof LobParam);
        assertNull(request.getQuantity());
        assertTrue(request.getSetting() instanceof SettingId);
    }

    @Test
    public void testCreateObjectLocalFile() throws Exception {
        final File file = ClientUtil.fileFromResource("goblue.pdf");

        final LobObjectRequest request = LobObjectRequest.builder()
            .name("Test Object")
            .file(file)
            .setting(200)
            .doubleSided(true)
            .template(true)
            .build();

        final LobObjectResponse response = client.createLobObject(request).get();

        assertTrue(response instanceof LobObjectResponse);
        assertThat(response.getName(), is("Test Object"));
    }

    @Test
    public void testRetrieveObject() throws Exception {
        final LobObjectResponseList responseList = client.getLobObjects(1).get();
        final LobObjectId id = Iterables.get(responseList, 0).getId();
        final LobObjectResponse retrievedResponse = client.getLobObject(id).get();

        assertTrue(retrievedResponse instanceof LobObjectResponse);
        assertThat(retrievedResponse.getId(), is(id));
    }
}
