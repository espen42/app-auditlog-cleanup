package handler;

import java.time.Instant;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.audit.CleanUpAuditLogResult;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
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
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;
import java.lang.Math;

public class CleanUpHandler
    implements ScriptBean
{

    private static final Logger LOG = LoggerFactory.getLogger( CleanUpHandler.class );

    private static final PrincipalKey NODE_SUPER_USER_KEY = PrincipalKey.ofUser( IdProviderKey.system(), "node-su" );

    private static final User NODE_SUPER_USER = User.create().key( NODE_SUPER_USER_KEY ).login( "node" ).build();

    private static final int BATCH_SIZE = 10_000;

    private Instant until;

    private NodeService nodeService;

    public CleanUpAuditLogResult run( final String until )
    {
        return callAsAdmin( () -> doRun( until ) );
    }

    private CleanUpAuditLogResult doRun( final String until )
    {
        this.until = Instant.parse( until );
        LOG.info( "Cleaning auditlog. Protect until: " + this.until );

        final CleanUpAuditLogResult.Builder result = CleanUpAuditLogResult.create();

        final NodeQuery query = createQuery();

        nodeService.refresh( RefreshMode.ALL );
        FindNodesByQueryResult nodesToDelete = nodeService.findByQuery( query );

        long hits = nodesToDelete.getHits();
        final long totalHits = nodesToDelete.getTotalHits();

        if ( totalHits == 0 )
        {
            LOG.info( "No auditlog entries to clean." );
            return CleanUpAuditLogResult.empty();
        }

        LOG.info( String.format( "Cleaning %d auditlog entries...", totalHits ) );

//        listener.start( BATCH_SIZE );

        int hitCount = 0;
        while ( hits > 0 )
        {
            for ( NodeHit nodeHit : nodesToDelete.getNodeHits() )
            {
                if ( hitCount % 1000 == 0 )
                {
                    LOG.info( String.format( "Cleaning auditlogs: %d - %d of total %d", hitCount, Math.min(hitCount+999, totalHits), totalHits ) );
                }

                try
                {
                    result.deleted( nodeService.deleteById( nodeHit.getNodeId() ).getSize() );

                }
                catch ( Exception e )
                {
                    LOG.error( String.format( "Can't remove node - %s", nodeHit.getNodeId() ) );
                }

                hitCount++;

//                listener.processed();
            }

            nodesToDelete = nodeService.findByQuery( query );

            hits = nodesToDelete.getHits();
        }

//        listener.finished();

        LOG.info( "Auditlog cleanups is done." );

        return result.build();
    }

    private NodeQuery createQuery()
    {
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

    private <T> T callAsAdmin( final Callable<T> runnable )
    {
        return ContextBuilder.from( ContextAccessor.current() )
            .authInfo( AuthenticationInfo.create().principals( NODE_SUPER_USER_KEY, RoleKeys.ADMIN ).user( NODE_SUPER_USER ).build() )
            .repositoryId( "system.auditlog" )
            .branch( "master" )
            .build()
            .callWith( runnable );
    }


    @Override
    public void initialize( final BeanContext context )
    {
        this.nodeService = context.getService( NodeService.class ).get();
    }
}
