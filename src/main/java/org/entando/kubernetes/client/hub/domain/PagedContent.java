package org.entando.kubernetes.client.hub.domain;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
public class PagedContent<T, P> {

    private List<T> payload;
    private Metadata<P> metadata;

    public PagedContent(List<T> payload, Page<P> pageObj) {
        this.payload = payload;
        this.metadata = new Metadata<>(pageObj);
    }

    public List<T> getPayload() {
        return payload;
    }

    public Metadata getMetadata() {
        return metadata;
    }


    @Data
    @NoArgsConstructor
    @Jacksonized
    public static class Metadata<P> {

        private int page;
        private int pageSize;
        private int lastPage;
        private long totalItems;

        public Metadata(Page<P> pageObj) {
            this.lastPage = pageObj.getTotalPages();
            this.totalItems = pageObj.getTotalElements();
            this.pageSize = pageObj.getSize();
            this.page = pageObj.getNumber() + 1;
        }

        public int getPage() {
            return page;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getLastPage() {
            return lastPage;
        }

        public long getTotalItems() {
            return totalItems;
        }
    }
}
