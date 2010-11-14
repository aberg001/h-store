package edu.brown.benchmark.auctionmark.procedures;

import org.voltdb.ProcInfo;
import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

import edu.brown.benchmark.auctionmark.AuctionMarkConstants;

/**
 * GetUserInfo Description goes here...
 * 
 * @author pavlo
 * @author visawee
 */
@ProcInfo(partitionInfo = "USER.U_ID: 0", singlePartition = false)
public class GetUserInfo extends VoltProcedure {

    public final SQLStmt select_user = new SQLStmt(
        "SELECT u_id, u_rating, u_created, u_sattr0, u_sattr1, u_sattr2, u_sattr3, u_sattr4, r_name " +
          "FROM " + AuctionMarkConstants.TABLENAME_USER + ", " +
                    AuctionMarkConstants.TABLENAME_REGION + " " +
         "WHERE u_id = ? AND u_r_id = r_id"
    );

    public final SQLStmt select_seller_items = new SQLStmt(
        "SELECT i_id, i_u_id, i_name, i_current_price, i_end_date, i_status " +
          "FROM " + AuctionMarkConstants.TABLENAME_ITEM + " " +
         "WHERE i_u_id = ? " +
         "ORDER BY i_end_date ASC LIMIT 20 "
    );
    
    public final SQLStmt select_buyer_items = new SQLStmt(
        "SELECT i_id, i_u_id, i_name, i_current_price, i_end_date, i_status " +
          "FROM " + AuctionMarkConstants.TABLENAME_USER_ITEM + ", " +
                    AuctionMarkConstants.TABLENAME_ITEM +
        " WHERE ui_u_id = ? " +
           "AND ui_i_id = i_id AND ui_i_u_id = i_u_id " +
         "ORDER BY i_end_date ASC LIMIT 10 "
    );

    public final SQLStmt select_seller_feedback = new SQLStmt(
        "SELECT if_rating, if_comment, if_date, " +
               "i_id, i_u_id, i_name, i_end_date, i_status, "+
               "u_id, u_rating, u_sattr0, u_sattr1 " +
          "FROM " + AuctionMarkConstants.TABLENAME_ITEM_FEEDBACK + ", " +
                    AuctionMarkConstants.TABLENAME_ITEM + ", " +
                    AuctionMarkConstants.TABLENAME_USER +
        " WHERE if_u_id = ? AND if_i_id = i_id AND if_u_id = i_u_id " +
           "AND if_buyer_id = u_id " +
        " ORDER BY if_date DESC "
    );

    /**
     * @param u_id
     * @param get_seller_items
     * @param get_feedback
     * @return
     */
    public VoltTable[] run(long u_id, long get_seller_items, long get_buyer_items, long get_feedback) {
        this.voltQueueSQL(this.select_user, u_id);
        final VoltTable user_results[] = this.voltExecuteSQL();
        assert (user_results.length == 1);

        // 33% of the time they're going to ask for additional information
        if (get_seller_items == 1 || get_buyer_items == 1) {
            // Of that 75% of the times we're going to get the seller's items
            if (get_seller_items == 1) {
                this.voltQueueSQL(this.select_seller_items, u_id);
            // And the remaining 25% of the time we'll get the buyer's purchased items
            } else if (get_buyer_items == 1) {
                this.voltQueueSQL(this.select_buyer_items, u_id);
            }
        }
            
        // Also get the user's feeback (33% of the time)
        if (get_feedback == 1) {
            this.voltQueueSQL(this.select_seller_feedback, u_id);
        }

        // Important: You have to make sure that none of the entries in the final
        // VoltTable results array that get passed back are null, otherwise
        // the ExecutionSite will throw an error!
        VoltTable results[] = null;
        if (get_seller_items == 1 || get_buyer_items == 1 || get_feedback == 1) {
            VoltTable extra_results[] = this.voltExecuteSQL();
            assert(extra_results.length > 0);

            results = new VoltTable[extra_results.length + 1];
            results[0] = user_results[0];
            for (int i = 0; i < extra_results.length; i++) {
                results[i+1] = extra_results[i];
            }
        } else {
            results = new VoltTable[] { user_results[0] };
        }
        return (results);
    }

}
