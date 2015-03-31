package com.lob.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.lob.Lob;
import com.lob.MoneyDeserializer;
import com.lob.id.JobId;
import com.lob.protocol.request.AreaMailRequest;
import com.lob.protocol.request.BankAccountRequest;
import com.lob.protocol.request.CheckRequest;
import com.lob.protocol.request.JobRequest;
import com.lob.protocol.request.ParamMappable;
import com.lob.protocol.request.PostcardRequest;
import com.lob.protocol.response.AreaMailResponse;
import com.lob.protocol.response.BankAccountResponse;
import com.lob.protocol.response.CheckResponse;
import com.lob.protocol.response.JobResponse;
import com.lob.protocol.response.JobResponseList;
import com.lob.protocol.response.PostcardResponse;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.FluentStringsMap;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Response;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.*;

public class AsyncLobClient implements LobClient {
    private final static ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JodaModule())
        .registerModule(new SimpleModule().addDeserializer(Money.class, new MoneyDeserializer(CurrencyUnit.USD)))
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final AsyncHttpClient httpClient;
    private final String baseUrl;
    private final String apiVersion;

    private final ExecutorService callbackExecutorService;

    private AsyncLobClient(
            final AsyncHttpClient httpClient,
            final String baseUrl,
            final String apiVersion,
            final ExecutorService callbackExecutorService) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.apiVersion = apiVersion;
        this.callbackExecutorService = callbackExecutorService;
    }

    public static LobClient createDefault(final String apiKey) {
        final Realm realm = new Realm.RealmBuilder()
            .setPrincipal(checkNotNull(apiKey))
            .setUsePreemptiveAuth(true)
            .setScheme(AuthScheme.BASIC)
            .build();

        final AsyncHttpClientConfig.Builder builder = new Builder();
        builder.setRealm(realm);

        return new AsyncLobClient(
            new AsyncHttpClient(builder.build()),
            Lob.getBaseUrl(),
            Lob.getApiVersion(),
            Executors.newCachedThreadPool());
    }

    @Override
    public ListenableFuture<JobResponse> createJob(final JobRequest jobRequest) {
        return execute(JobResponse.class, post(Router.JOBS, jobRequest), this.callbackExecutorService);
    }

    @Override
    public ListenableFuture<JobResponse> getJob(final JobId id) {
        return execute(JobResponse.class, get(Router.JOBS + "/" + id.value()), this.callbackExecutorService);
    }

    @Override
    public ListenableFuture<JobResponseList> getAllJobs() {
        return execute(JobResponseList.class, get(Router.JOBS), this.callbackExecutorService);
    }

    @Override
    public ListenableFuture<JobResponseList> getJobs(final int count) {
        return execute(JobResponseList.class, get(Router.JOBS, new FluentStringsMap().add("count", Integer.toString(count))), this.callbackExecutorService);
    }

    @Override
    public ListenableFuture<JobResponseList> getJobs(final int count, final int offset) {
        return execute(
            JobResponseList.class,
            get(Router.JOBS,
                new FluentStringsMap()
                    .add("count", Integer.toString(count))
                    .add("offset", Integer.toString(offset))
            ),
            this.callbackExecutorService);
    }

    @Override
    public ListenableFuture<PostcardResponse> createPostcard(final PostcardRequest postcardRequest) {
        return execute(PostcardResponse.class, post(Router.POSTCARDS, postcardRequest), this.callbackExecutorService);
    }

    @Override
    public ListenableFuture<CheckResponse> createCheck(final CheckRequest checkRequest) {
        return execute(CheckResponse.class, post(Router.CHECKS, checkRequest), this.callbackExecutorService);
    }

    @Override
    public ListenableFuture<BankAccountResponse> createBankAccount(final BankAccountRequest bankAccountRequest) {
        return execute(BankAccountResponse.class, post(Router.BANK_ACCOUNTS, bankAccountRequest), this.callbackExecutorService);
    }

    @Override
    public ListenableFuture<AreaMailResponse> createAreaMail(final AreaMailRequest areaMailRequest) {
        return execute(AreaMailResponse.class, post(Router.AREA_MAIL, areaMailRequest), this.callbackExecutorService);
    }

    private BoundRequestBuilder get(final String resourceUrl) {
        return get(resourceUrl, new FluentStringsMap());
    }

    private BoundRequestBuilder get(final String resourceUrl, final FluentStringsMap params) {
        return this.httpClient.prepareGet(this.baseUrl + resourceUrl).setQueryParameters(params);
    }

    private BoundRequestBuilder post(final String resourceUrl, final ParamMappable request) {
        return this.httpClient.preparePost(this.baseUrl + resourceUrl).setParameters(request.toParamMap());
    }

    private static <T> ListenableFuture<T> execute(
        final Class<T> clazz,
        final BoundRequestBuilder request,
        final ExecutorService callbackExecutorService) {
        final SettableFuture<T> guavaFut = SettableFuture.create();
        try {
            request.execute(new GuavaFutureConverter<T>(clazz, guavaFut, callbackExecutorService));
        }
        catch (final IOException e) {
            guavaFut.setException(e);
        }
        return guavaFut;
    }

    private static class GuavaFutureConverter<T> extends AsyncCompletionHandler<T> {
        final Class<T> clazz;
        final SettableFuture<T> guavaFut;
        final ExecutorService callbackExecutorService;

        public GuavaFutureConverter(
                final Class<T> clazz,
                final SettableFuture<T> guavaFut,
                final ExecutorService callbackExecutorService) {
            this.clazz = clazz;
            this.guavaFut = guavaFut;
            this.callbackExecutorService = callbackExecutorService;
        }

        @Override
        public T onCompleted(final Response response) throws Exception {
            final T value = MAPPER.readValue(response.getResponseBody(), clazz);
            // Execute setting the guava future in a separate thread so any callbacks
            // executed on the guava future don't block the ning IO threads.
            this.callbackExecutorService.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        guavaFut.set(value);
                    }
                });
            return value;
        }
    }
}
