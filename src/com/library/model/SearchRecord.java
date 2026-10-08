package com.library.model;

public class SearchRecord {
    private String id;
    private String memberId;
    private String query;
    private String searchCriteria; // "title", "author", "genre", "all"
    private int resultCount;
    private String timestamp;

    public SearchRecord() {}

    public SearchRecord(String id, String memberId, String query, String searchCriteria, int resultCount, String timestamp) {
        this.id = id;
        this.memberId = memberId;
        this.query = query;
        this.searchCriteria = searchCriteria;
        this.resultCount = resultCount;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getSearchCriteria() { return searchCriteria; }
    public void setSearchCriteria(String searchCriteria) { this.searchCriteria = searchCriteria; }

    public int getResultCount() { return resultCount; }
    public void setResultCount(int resultCount) { this.resultCount = resultCount; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
