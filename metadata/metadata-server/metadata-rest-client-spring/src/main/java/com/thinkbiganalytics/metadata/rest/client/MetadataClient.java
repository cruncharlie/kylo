/**
 * 
 */
package com.thinkbiganalytics.metadata.rest.client;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.thinkbiganalytics.metadata.rest.model.data.Datasource;
import com.thinkbiganalytics.metadata.rest.model.data.DatasourceCriteria;
import com.thinkbiganalytics.metadata.rest.model.data.DirectoryDatasource;
import com.thinkbiganalytics.metadata.rest.model.data.HiveTableDatasource;
import com.thinkbiganalytics.metadata.rest.model.data.HiveTableField;
import com.thinkbiganalytics.metadata.rest.model.data.HiveTablePartition;
import com.thinkbiganalytics.metadata.rest.model.feed.Feed;
import com.thinkbiganalytics.metadata.rest.model.feed.FeedCriteria;
import com.thinkbiganalytics.metadata.rest.model.feed.FeedDependency;
import com.thinkbiganalytics.metadata.rest.model.feed.FeedPrecondition;
import com.thinkbiganalytics.metadata.rest.model.op.DataOperation;
import com.thinkbiganalytics.metadata.rest.model.sla.Metric;
import com.thinkbiganalytics.metadata.rest.model.sla.ServiceLevelAssessment;

/**
 *
 * @author Sean Felten
 */
public class MetadataClient {
    
    public static final List<MediaType> ACCEPT_TYPES = Collections.unmodifiableList(Arrays.asList(MediaType.APPLICATION_JSON));
    
    public static final ParameterizedTypeReference<List<Feed>> FEED_LIST = new ParameterizedTypeReference<List<Feed>>() { };
    public static final ParameterizedTypeReference<List<Datasource>> DATASOURCE_LIST = new ParameterizedTypeReference<List<Datasource>>() { };
    public static final ParameterizedTypeReference<List<Metric>> METRIC_LIST = new ParameterizedTypeReference<List<Metric>>() { };
    
    private static final Function<UriComponentsBuilder, UriComponentsBuilder> ALL_DATASOURCES = new TargetDatasourceCriteria();
    private static final Function<UriComponentsBuilder, UriComponentsBuilder> ALL_FEEDS = new TargetFeedCriteria();
    
    private final URI base;
    private final RestTemplate template;
    
    public MetadataClient(URI base) {
        super();
        this.base = base;
        this.template = new RestTemplate();
        
        ObjectMapper mapper = createObjectMapper();
//        this.template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        this.template.getMessageConverters().add(new MappingJackson2HttpMessageConverter(mapper));
    }
    
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        mapper.setSerializationInclusion(Include.NON_NULL);
        return mapper;
    }

    public FeedBuilder buildFeed(String name) {
        return new FeedBuilderImpl(name);
    }
    
    public Feed addSource(String feedId, String datasourceId) {
        Form form = new Form();
        form.add("datasourceId", datasourceId);
        
        return post(Paths.get("feed", feedId, "source"), form, Feed.class);
    }
    
    public Feed addDestination(String feedId, String datasourceId) {
        Form form = new Form();
        form.add("datasourceId", datasourceId);
        
        return post(Paths.get("feed", feedId, "destination"), form, Feed.class);
    }

    public Feed setPrecondition(String feedId, Metric... metrics) {
        return setPrecondition(feedId, Arrays.asList(metrics));
    }
    
    public Feed setPrecondition(String feedId, List<Metric> metrics) {
        FeedPrecondition precond = new FeedPrecondition(metrics);
        return setPrecondition(feedId, precond);
    }
    
    public Feed setPrecondition(String feedId, FeedPrecondition precond) {
        return post(Paths.get("feed", feedId, "precondition"), precond, Feed.class);
    }

    public FeedCriteria feedCriteria() {
        return new TargetFeedCriteria();
    }
    
    public List<Feed> getFeeds() {
        return getFeeds((FeedCriteria) ALL_FEEDS);
    }
    
    public List<Feed> getFeeds(FeedCriteria criteria) {
        try {
            return get(Paths.get("feed"), (TargetFeedCriteria) criteria, FEED_LIST);
        } catch (ClassCastException e) {
            throw new IllegalThreadStateException("Unknown criteria type: " + criteria.getClass());
        }
    }

    public Feed getFeed(String id) {
        return get(Paths.get("feed", id), Feed.class);
    }
    
    public FeedDependency getFeedDependency(String id) {
        return get(Paths.get("feed", id, "depfeeds"), FeedDependency.class);
    }

    public DirectoryDatasourceBuilder buildDirectoryDatasource(String name) {
        return new DirectoryDatasourceBuilderImpl(name);
    }
    
    public HiveTableDatasourceBuilder buildHiveTableDatasource(String name) {
        return new HiveTableDatasourceBuilderImpl(name);
    }
    
    public DatasourceCriteria datasourceCriteria() {
        return new TargetDatasourceCriteria();
    }

    public List<Datasource> getDatasources() {
        return get(Paths.get("datasource"), ALL_DATASOURCES, DATASOURCE_LIST);
    }
    
    public List<Datasource> getDatasources(DatasourceCriteria criteria) {
        try {
            return get(Paths.get("datasource"), (TargetDatasourceCriteria) criteria, DATASOURCE_LIST);
        } catch (ClassCastException e) {
            throw new IllegalThreadStateException("Unknown criteria type: " + criteria.getClass());
        }
    }

    public DataOperation beginOperation(String feedDestinationId, String status) {
        Form form = new Form();
        form.add("feedDestinationId", feedDestinationId);
        form.add("status", status);
        
        return post(Paths.get("dataop"), form, DataOperation.class);
    }
    
    public DataOperation updateDataOperation(DataOperation op) {
        return put(Paths.get("dataop", op.getId()), op, DataOperation.class);
    }
    
    public DataOperation getDataOperation(String id) {
        return get(Paths.get("dataop", id), DataOperation.class);
    }

    public ServiceLevelAssessment assessPrecondition(String id) {
        return get(Paths.get("feed", id, "precondition", "assessment"), ServiceLevelAssessment.class);
    }
    
    public String getPreconditionResult(String id) {
        return get(Paths.get("feed", id, "precondition", "assessment", "result"), String.class);
    }

    private FeedPrecondition createTrigger(List<Metric> metrics) {
        if (! metrics.isEmpty()) {
            FeedPrecondition trigger = new FeedPrecondition();
            trigger.addMetrics(metrics);
            return trigger;
        } else {
            return null;
        }
    }

    private Feed postFeed(Feed feed) {
        return post(Paths.get("feed"), feed, Feed.class);
    }
    
    private HiveTableDatasource postDatasource(HiveTableDatasource ds) {
        return post(Paths.get("datasource", "hivetable"), ds, HiveTableDatasource.class);
    }
    
    private DirectoryDatasource postDatasource(DirectoryDatasource ds) {
        return post(Paths.get("datasource", "directory"), ds, DirectoryDatasource.class);
    }
    
    private UriComponentsBuilder base(Path path) {
        return UriComponentsBuilder.fromUri(this.base).path("/").path(path.toString());
    }
    
    private <R> R get(Path path, Class<R> resultType) {
        return get(path, null, resultType);
    }
    
    private <R> R get(Path path, Function<UriComponentsBuilder, UriComponentsBuilder> filterFunct, Class<R> resultType) {
        return this.template.getForObject(
                (filterFunct != null ? filterFunct.apply(base(path)) : base(path)).build().toUri(),
                resultType);
    }
    
    private <R> R get(Path path, Function<UriComponentsBuilder, UriComponentsBuilder> filterFunct, ParameterizedTypeReference<R> responseEntity) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(ACCEPT_TYPES);
        
        ResponseEntity<R> resp = this.template.exchange(
                (filterFunct != null ? filterFunct.apply(base(path)) : base(path)).build().toUri(),
                HttpMethod.GET,
                new HttpEntity<Object>(headers),
                responseEntity);
        
        return handle(resp);
    }
    
    private <R> R post(Path path, Form form, Class<R> resultType) {
        return this.template.postForObject(base(path).build().toUri(), form, resultType);
    }
    
    private <R> R post(Path path, Object body, Class<R> resultType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(ACCEPT_TYPES);
        
        return this.template.postForObject(base(path).build().toUri(), 
                                           new HttpEntity<>(body, headers), 
                                           resultType);
    }
    
    private <R> R put(Path path, Object body, Class<R> resultType) {
        URI uri = base(path).build().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(ACCEPT_TYPES);
        
        this.template.put(uri, new HttpEntity<>(body, headers));
        // Silly that put() doesn't return an object.
        return get(path, resultType);
    }
    

    private <R> R handle(ResponseEntity<R> resp) {
        if (resp.getStatusCode().is2xxSuccessful()) {
            return resp.getBody();
        } else {
            throw new WebResponseException(ResponseEntity.status(resp.getStatusCode()).headers(resp.getHeaders()).build());
        }
    }

    
    private static class Form extends LinkedMultiValueMap<String, String> {
    }


    private class FeedBuilderImpl implements FeedBuilder {
        private String displayName;
        private String systemName;
        private String description;
        private String owner;
        private List<Metric> preconditionMetrics = new ArrayList<>();
    
        public FeedBuilderImpl(String name) {
            this.systemName = name;
        }
    
        @Override
        public FeedBuilder displayName(String name) {
            this.displayName = name;
            return this;
        }
    
        @Override
        public FeedBuilder description(String descr) {
            this.description = descr;
            return this;
        }
    
        @Override
        public FeedBuilder owner(String owner) {
            this.owner = owner;
            return this;
        }
    
        @Override
        public FeedBuilder preconditionMetric(Metric... metrics) {
            for (Metric m : metrics) {
                this.preconditionMetrics.add(m);
            }
            return this;
        }
    
        @Override
        public Feed build() {
            Feed feed = new Feed();
            feed.setSystemName(this.systemName);
            feed.setDisplayName(this.displayName != null ? this.displayName : this.systemName);
            feed.setDescription(this.description);
            feed.setOwner(this.owner);
            feed.setPrecondition(createTrigger(this.preconditionMetrics));
            
            return feed;
        }
        
        @Override
        public Feed post() {
            Feed feed = build();
            return postFeed(feed);
        }
    
    }

    private abstract class DatasourceBuilderImpl<B extends DatasourceBuilder<B, D>, D extends Datasource> implements DatasourceBuilder<B, D> {
        protected String name;
        protected String description;
        protected String owner;
        protected boolean encrypted;
        protected boolean compressed;
        
        public DatasourceBuilderImpl(String name) {
            this.name = name;
        }
        
        @Override
        public B description(String descr) {
            this.description = descr;
            return self();
        }

        @Override
        public B ownder(String owner) {
            this.owner = owner;
            return self();
        }

        @Override
        public B encrypted(boolean flag) {
            this.encrypted = flag;
            return self();
        }

        @Override
        public B compressed(boolean flag) {
            this.compressed = flag;
            return self();
        }

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }
    }
    
    private class HiveTableDatasourceBuilderImpl
            extends DatasourceBuilderImpl<HiveTableDatasourceBuilder, HiveTableDatasource>
            implements HiveTableDatasourceBuilder {
        
        private String database;
        private String tableName;
        private String modifiers;
        private List<HiveTableField> fields = new ArrayList<>(); 
        private List<HiveTablePartition> partitions = new ArrayList<>();

        public HiveTableDatasourceBuilderImpl(String name) {
            super(name);
        }

        @Override
        public HiveTableDatasourceBuilder database(String name) {
            this.database = name;
            return this;
        }

        @Override
        public HiveTableDatasourceBuilder tableName(String name) {
            this.tableName = name;
            return this;
        }

        @Override
        public HiveTableDatasourceBuilder modifiers(String mods) {
            this.modifiers = mods;
            return this;
        }

        @Override
        public HiveTableDatasourceBuilder field(String name, String type) {
            this.fields.add(new HiveTableField(name, type));
            return this;
        }

        @Override
        public HiveTableDatasourceBuilder partition(String name, String formula, String value, String... more) {
            this.partitions.add(new HiveTablePartition(name, formula, value, more));
            return this;
        }

        @Override
        public HiveTableDatasource build() {
            HiveTableDatasource src = new HiveTableDatasource();
            src.setName(this.name);
            src.setDescription(this.description);
            src.setOwnder(this.owner);
            src.setEncrypted(this.encrypted);
            src.setCompressed(this.compressed);
            src.setDatabase(this.database);
            src.setTableName(this.tableName);
            src.setModifiers(this.modifiers);
            src.getFields().addAll(this.fields);
            src.getPartitions().addAll(this.partitions);

            return src;
        }

        @Override
        public HiveTableDatasource post() {
            HiveTableDatasource ds = build();
            return postDatasource(ds);
        }
    }
    
    private class DirectoryDatasourceBuilderImpl 
            extends DatasourceBuilderImpl<DirectoryDatasourceBuilder, DirectoryDatasource> 
            implements DirectoryDatasourceBuilder {
    
        private String path;
        private List<String> regexList = new ArrayList<>();
        private List<String> globList = new ArrayList<>();
        
        public DirectoryDatasourceBuilderImpl(String name) {
            super(name);
        }
    
        @Override
        public DirectoryDatasourceBuilder path(String path) {
            this.path = path;
            return this;
        }
    
        @Override
        public DirectoryDatasourceBuilder regexPattern(String pattern) {
            this.regexList.add(pattern);
            return this;
        }
    
        @Override
        public DirectoryDatasourceBuilder globPattern(String pattern) {
            this.globList.add(pattern);
            return this;
        }
        
        @Override
        public DirectoryDatasource build() {
            DirectoryDatasource src = new DirectoryDatasource();
            src.setName(this.name);
            src.setDescription(this.description);
            src.setOwnder(this.owner);
            src.setEncrypted(this.encrypted);
            src.setCompressed(this.compressed);
            src.setPath(this.path);
            
            for (String p : this.regexList) {
                src.addRegexPattern(p);
            }
            
            for (String p : this.globList) {
                src.addGlobPattern(p);
            }
            
            return src;
        }
    
        @Override
        public DirectoryDatasource post() {
            DirectoryDatasource dds = build();
            return postDatasource(dds);
        }
        
    }

    private static class TargetDatasourceCriteria implements DatasourceCriteria, Function<UriComponentsBuilder, UriComponentsBuilder> {
        
        private String name;
        private String owner;
        private DateTime createdOn;
        private DateTime createdAfter;
        private DateTime createdBefore;
        private Set<String> types = new HashSet<>();
        
        public UriComponentsBuilder apply(UriComponentsBuilder target) {
            UriComponentsBuilder result = target;
            
            if (! Strings.isNullOrEmpty(this.name)) result = result.queryParam(NAME, this.name);
            if (! Strings.isNullOrEmpty(this.owner)) result = result.queryParam(OWNER, this.owner);
            if (this.createdOn != null) result = result.queryParam(ON, this.createdOn.toString());
            if (this.createdAfter != null) result = result.queryParam(AFTER, this.createdAfter.toString());
            if (this.createdBefore != null) result = result.queryParam(BEFORE, this.createdBefore.toString());
            if (! this.types.isEmpty()) result = result.queryParam(TYPE, types.toArray(new Object[types.size()]));
            
            return result;
        }
        
        /* (non-Javadoc)
         * @see com.thinkbiganalytics.metadata.rest.client.DatasourceCriteria#name(java.lang.String)
         */
        @Override
        public DatasourceCriteria name(String name) {
            this.name = name;
            return this;
        }

        /* (non-Javadoc)
         * @see com.thinkbiganalytics.metadata.rest.client.DatasourceCriteria#createdOn(org.joda.time.DateTime)
         */
        @Override
        public DatasourceCriteria createdOn(DateTime time) {
            this.createdOn = time;
            return this;
        }

        /* (non-Javadoc)
         * @see com.thinkbiganalytics.metadata.rest.client.DatasourceCriteria#createdAfter(org.joda.time.DateTime)
         */
        @Override
        public DatasourceCriteria createdAfter(DateTime time) {
            this.createdAfter = time;
            return this;
        }

        /* (non-Javadoc)
         * @see com.thinkbiganalytics.metadata.rest.client.DatasourceCriteria#createdBefore(org.joda.time.DateTime)
         */
        @Override
        public DatasourceCriteria createdBefore(DateTime time) {
            this.createdBefore = time;
            return this;
        }

        /* (non-Javadoc)
         * @see com.thinkbiganalytics.metadata.rest.client.DatasourceCriteria#owner(java.lang.String)
         */
        @Override
        public DatasourceCriteria owner(String owner) {
            this.owner = owner;
            return this;
        }

        /* (non-Javadoc)
         * @see com.thinkbiganalytics.metadata.rest.client.DatasourceCriteria#type(java.lang.Class, java.lang.Class[])
         */
        @Override
        @SuppressWarnings("unchecked")
        public DatasourceCriteria type(Class<? extends Datasource> type, Class<? extends Datasource>... others) {
            this.types.add(type.getSimpleName());
            for (Class<? extends Datasource> t : others) {
                this.types.add(t.getSimpleName());
            }
            return this;
        }
    }

    private static class TargetFeedCriteria implements FeedCriteria, Function<UriComponentsBuilder, UriComponentsBuilder> {
        
        private String name;
        private String sourceId;
        private String destinationId;

        public UriComponentsBuilder apply(UriComponentsBuilder target) {
            UriComponentsBuilder result = target;
            
            if (! Strings.isNullOrEmpty(this.name)) result = result.queryParam(NAME, this.name);
            if (! Strings.isNullOrEmpty(this.sourceId)) result = result.queryParam(SRC_ID, this.name);
            if (! Strings.isNullOrEmpty(this.destinationId)) result = result.queryParam(DEST_ID, this.name);
            
            return result;
        }

        /* (non-Javadoc)
         * @see com.thinkbiganalytics.metadata.rest.client.FeedCriteria#sourceDatasource(java.lang.String)
         */
        @Override
        public FeedCriteria sourceDatasource(String dsId) {
            this.sourceId = dsId;
            return this;
        }

        /* (non-Javadoc)
         * @see com.thinkbiganalytics.metadata.rest.client.FeedCriteria#destinationDatasource(java.lang.String)
         */
        @Override
        public FeedCriteria destinationDatasource(String dsId) {
            this.destinationId = dsId;
            return this;
        }

        /* (non-Javadoc)
         * @see com.thinkbiganalytics.metadata.rest.client.FeedCriteria#name(java.lang.String)
         */
        @Override
        public FeedCriteria name(String name) {
            this.name = name;
            return this;
        }
    }


}