package fieldtest;

public class A2PK implements java.io.Serializable {

        public String id1;
        public java.util.Date iddate;

        public A2PK() {}
        
        public boolean equals(java.lang.Object obj) {
            if( obj==null ||
            !this.getClass().equals(obj.getClass()) ) return( false );
            A2PK o=(A2PK) obj;
            if( !this.id1.equals(o.id1) || !this.iddate.equals(o.iddate) ) return( false );
            return( true );
        }
        
        public int hashCode() {
            int hashCode=0;
            hashCode += id1.hashCode() + iddate.hashCode();
            return( hashCode );
        }
}
