package handler;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.index.ChildOrder;
import com.enonic.xp.index.IndexPath;
import com.enonic.xp.node.NodeType;
import com.enonic.xp.query.expr.FieldOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;

public class AuditLogConstants
{
    public static final RepositoryId AUDIT_LOG_REPO_ID = RepositoryId.from( "system.auditlog" );

    public static final Branch AUDIT_LOG_BRANCH = Branch.create().
        value( "master" ).
        build();

    public static final AccessControlList AUDIT_LOG_REPO_DEFAULT_ACL = AccessControlList.create().
        add( AccessControlEntry.create().
            allowAll().
            principal( RoleKeys.ADMIN ).
            build() ).
        add( AccessControlEntry.create().
            allowAll().
            principal( RoleKeys.AUDIT_LOG ).
            build() ).
        build();

    public static final IndexPath TIME = IndexPath.from( "time" );

    public static final ChildOrder AUDIT_LOG_REPO_DEFAULT_CHILD_ORDER = ChildOrder.create().
        add( FieldOrderExpr.create( TIME, OrderExpr.Direction.DESC ) ).
        build();

    public static final NodeType NODE_TYPE = NodeType.from( "auditlog");
}
