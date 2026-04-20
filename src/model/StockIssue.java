package model;

public class StockIssue {
    private int productId;
    private int availableStock;

    public StockIssue(int productId, int availableStock) {
        this.productId = productId;
        this.availableStock = availableStock;
    }

    public int getProductId() {
        return productId;
    }

    public int getAvailableStock() {
        return availableStock;
    }
}