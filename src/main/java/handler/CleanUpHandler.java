package handler;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.audit.CleanUpAuditLogResult;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.NodeHit;
import com.enonic.xp.node.NodeIndexPath;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.RefreshMode;
import com.enonic.xp.query.expr.FieldOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;
import com.enonic.xp.query.filter.RangeFilter;
import com.enonic.xp.query.filter.ValueFilter;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class CleanUpHandler
    implements ScriptBean
{

    private static final Logger LOG = LoggerFactory.getLogger( CleanUpHandler.class );

    private static final int BATCH_SIZE = 10_000;

    private Instant until;

    private NodeService nodeService;

    public CleanUpAuditLogResult run( final String until )
    {
        this.until = Instant.parse( until );
        LOG.info("Until: " + this.until);

        final CleanUpAuditLogResult.Builder result = CleanUpAuditLogResult.create();

        final NodeQuery query = createQuery();

        LOG.info("query: " + query);

        nodeService.refresh( RefreshMode.ALL );
        FindNodesByQueryResult nodesToDelete = nodeService.findByQuery( query );

        long hits = nodesToDelete.getHits();
        final long totalHits = nodesToDelete.getTotalHits();


        if ( totalHits == 0 )
        {
            LOG.info( "No auditlog entries to clean.");
            return CleanUpAuditLogResult.empty();
        }

        LOG.info( String.format( "Cleaning %d auditlog entries...", totalHits));

//        listener.start( BATCH_SIZE );

        int hitCount = 0;
        while ( hits > 0 )
        {
            for ( NodeHit nodeHit : nodesToDelete.getNodeHits() )
            {
                if (hitCount%100 == 0) {
                    LOG.info( String.format( "Cleaning auditlog: %d / %d", hitCount, totalHits));
                }

                try {
                    result.deleted( nodeService.deleteById( nodeHit.getNodeId() ).getSize() );

                } catch ( Exception e ) {
                    LOG.error( String.format( "Can't remove node - %s", nodeHit.getNodeId() ) );
                }

                hitCount++;

//                listener.processed();
            }

            nodesToDelete = nodeService.findByQuery( query );

            hits = nodesToDelete.getHits();
        }

//        listener.finished();

        LOG.info("Auditlog cleanups is done.");

        return result.build();
    }

    private NodeQuery createQuery()
    {
        LOG.info("AuditLogConstants time: " + AuditLogConstants.TIME.toString() );
        LOG.info("NodeIndexPath nodetype: " + NodeIndexPath.NODE_TYPE.toString() );
        LOG.info("AuditLogConstants nodetype: " + AuditLogConstants.NODE_TYPE.toString() );
        LOG.info("AuditLogConstants nodetype value: " + ValueFactory.newString( AuditLogConstants.NODE_TYPE.toString() ));

        final NodeQuery.Builder builder = NodeQuery.create()
            .addQueryFilter( ValueFilter.create()
                                 .fieldName( NodeIndexPath.NODE_TYPE.toString() )
                                 .addValue( ValueFactory.newString( AuditLogConstants.NODE_TYPE.toString() ) )
                                 .build() );

        final RangeFilter timeToFilter =
            RangeFilter.create().fieldName( AuditLogConstants.TIME.toString() ).to( ValueFactory.newDateTime( until ) ).build();
        builder.addQueryFilter( timeToFilter );

        builder.addOrderBy( FieldOrderExpr.create( AuditLogConstants.TIME, OrderExpr.Direction.ASC ) ).size( BATCH_SIZE );

        return builder.build();
    }


    @Override
    public void initialize( final BeanContext context )
    {
        this.nodeService = context.getService( NodeService.class ).get();
    }
}
