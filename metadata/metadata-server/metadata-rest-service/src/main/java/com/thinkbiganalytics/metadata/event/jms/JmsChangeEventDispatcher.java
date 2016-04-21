/**
 * 
 */
package com.thinkbiganalytics.metadata.event.jms;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.Topic;

import org.springframework.jms.core.JmsMessagingTemplate;

import com.thinkbiganalytics.metadata.api.datasource.Datasource;
import com.thinkbiganalytics.metadata.api.op.Dataset;
import com.thinkbiganalytics.metadata.api.op.ChangeSet;
import com.thinkbiganalytics.metadata.core.feed.FeedPreconditionService;
import com.thinkbiganalytics.metadata.core.feed.PreconditionEvent;
import com.thinkbiganalytics.metadata.core.feed.PreconditionListener;
import com.thinkbiganalytics.metadata.event.SimpleChangeEventDispatcher;
import com.thinkbiganalytics.metadata.rest.Model;
import com.thinkbiganalytics.metadata.rest.model.event.DatasourceChangeEvent;
import com.thinkbiganalytics.metadata.rest.model.feed.Feed;

/**
 *
 * @author Sean Felten
 */
public class JmsChangeEventDispatcher extends SimpleChangeEventDispatcher implements PreconditionListener {
    
    // TODO listen for change events and write to datasourceChangeTopic?
    
    @Inject
    @Named("preconditionTriggerTopic")
    private Topic preconditionTriggerTopic;
    
    @Inject
    @Named("metadataMessagingTemplate")
    private JmsMessagingTemplate jmsMessagingTemplate;
    
    @Inject
    private FeedPreconditionService preconditionService;

    @PostConstruct
    public void listenForPreconditions() {
        this.preconditionService.addListener(this);
    }

    @Override
    public void triggered(PreconditionEvent preEvent) {
        Feed feed = Model.DOMAIN_TO_FEED.apply(preEvent.getFeed());
        DatasourceChangeEvent dsEvent = new DatasourceChangeEvent(feed);
        
        for (Dataset<Datasource, ChangeSet> cs : preEvent.getDatasets()) {
            com.thinkbiganalytics.metadata.rest.model.op.Dataset dset = Model.DOMAIN_TO_DATASET.apply(cs);
            dsEvent.addDataset(dset);
        }
        
        this.jmsMessagingTemplate.convertAndSend(preconditionTriggerTopic, dsEvent);
//        this.jmsMessagingTemplate.send(this.preconditionTriggerTopic, null);
//        this.jmsSender.sendObject(preconditionTriggerTopic, dsEvent);
//        this.jmsSender.sendMessage(preconditionTriggerTopic, "test message");
    }
}