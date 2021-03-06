package net.resthub.model;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@XmlRootElement
@Entity
@Getter @Setter @ToString
@EqualsAndHashCode(callSuper = true, of = { "namespace", "name" })
@Table(name = "HUB_TABLE", uniqueConstraints = { 
    @UniqueConstraint(columnNames = {"NAMESPACE", "NAME"})})
public class MdTable extends MdEntity {   
    
    public static final int ETERNAL_CACHE_TIME = -1;
    public static final int SKIP_CACHE_TIME = 0;
    public static final int DEFAULT_CACHE_TIME = 120;
    public static final int DEFAULT_HIT_COUNT = 1;
    public static final int DEFAULT_TIME_OUT = 30;
    public static final int MAX_ROWS_LIMIT = 1000;
    public static final int DEFAULT_ROWS_LIMIT = MAX_ROWS_LIMIT;
    
    @Basic
    @Column(name = "NAMESPACE", nullable = false, length = 30)
    @XmlElement(name = "NAMESPACE")
    private String namespace;
    
    @Basic
    @Column(name = "NAME", nullable = false, length = 30)
    @XmlElement(name = "NAME")
    private String name;
    
    @Basic
    @Lob
    @Column(name = "SQL_VALUE", nullable = false)
    @XmlElement(name = "SQL")
    private String sql;    
    
    @Basic
    @Column(name = "CACHE_TIME", nullable = false)
    @XmlAttribute(name = "CACHE_TIME")
    private Integer cacheTime = DEFAULT_CACHE_TIME;
    
    @Basic
    @Column(name = "HIT_COUNT", nullable = false)
    @XmlAttribute(name = "HIT_COUNT")
    private Integer hitCount = DEFAULT_HIT_COUNT;
    
    @Basic
    @Column(name = "TIME_OUT", nullable = false)
    @XmlAttribute(name = "TIME_OUT")
    private Integer timeout = DEFAULT_TIME_OUT;
    
    @Basic
    @Column(name = "ROWS_LIMIT", nullable = false)
    @XmlAttribute(name = "ROWS_LIMIT")
    private Integer rowsLimit = DEFAULT_ROWS_LIMIT;

    @Basic
    @Column(name = "CONNECTION_NAME", nullable = false, length = 30)
    @XmlAttribute(name = "CONNECTION_NAME")
    private String connectionName;
    
    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @OrderBy("number ASC")
    @XmlElementWrapper(name = "COLUMNS")
    @XmlElement(name = "COLUMN")
    private List<MdColumn> columns = new ArrayList<>();
    
    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @OrderBy("name ASC")
    @XmlElementWrapper(name = "PARAMETERS")
    @XmlElement(name = "PARAMETER")
    private List<MdParameter> parameters = new ArrayList<>();
    
}
