package com.example.wapp;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wapp.adapter.CartAdapter;
import com.example.wapp.adapter.CartItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private RecyclerView cartRecyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItemList;
    private ListenerRegistration cartListenerRegistration;
    private TextView totalPriceTextView;
    private Button checkoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.cart);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.bottom_home:
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                    return true;
                case R.id.cart:
                    return true;
                case R.id.orders:
                    startActivity(new Intent(getApplicationContext(), OrdersActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                    return true;
                case R.id.profile:
                    startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                    return true;

            }
            return false;
        });

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        cartRecyclerView = findViewById(R.id.cartRecyclerView);
        totalPriceTextView = findViewById(R.id.totalPriceTextView);
        checkoutButton = findViewById(R.id.checkoutButton);

        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        cartItemList = new ArrayList<>();
        cartAdapter = new CartAdapter(cartItemList, this);
        cartRecyclerView.setAdapter(cartAdapter);

        // Replace `userId` with your actual user ID value
        String userId = mAuth.getCurrentUser().getUid();
        CollectionReference cartItemsRef = firestore.collection("users").document(userId).collection("cart");
        cartItemsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                cartItemList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String itemId = document.getId();
                    String name = document.getString("name");
                    double price = document.getDouble("price");
                    String imageString = document.getString("image");

                    // Convert the Base64 string to a byte array
                    byte[] imageBytes = Base64.decode(imageString, Base64.DEFAULT);

                    CartItem cartItem = new CartItem(itemId, name, price, imageBytes);
                    cartItemList.add(cartItem);
                }
                cartAdapter.notifyDataSetChanged();
                updateTotalPrice();
            } else {
                // Handle the error
                Log.e(TAG, "Error getting documents: ", task.getException());
            }
        });

        CollectionReference cartRef = firestore.collection("users").document(userId).collection("cart");

        cartListenerRegistration = cartRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening for cart changes: ", error);
                return;
            }

            // Clear the existing list
            cartItemList.clear();

            // Iterate over the snapshot documents and populate the list
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                String itemId = document.getId();
                String name = document.getString("name");
                double price = document.getDouble("price");
                String imageString = document.getString("image");

                // Convert the Base64 string to a byte array
                byte[] imageBytes = Base64.decode(imageString, Base64.DEFAULT);

                CartItem cartItem = new CartItem(itemId, name, price, imageBytes);
                cartItemList.add(cartItem);
            }

            // Notify the adapter that the data set has changed
            cartAdapter.notifyDataSetChanged();
            updateTotalPrice();
        });

        checkoutButton.setOnClickListener(view -> {
            Toast.makeText(getApplicationContext(),"Order Placed Successfully",Toast.LENGTH_SHORT).show();
            Intent i = new Intent(getApplicationContext(),OrdersActivity.class);
            startActivity(i);
            setContentView(R.layout.activity_orders);


        });

    }

    private void updateTotalPrice() {
        double total = 0.0;
        for (CartItem cartItem : cartItemList) {
            total += cartItem.getPrice();
        }

        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        String totalPrice = decimalFormat.format(total);
        totalPriceTextView.setText("Total: $" + totalPrice);
    }

    public void removeItem(int position) {
        cartItemList.remove(position);
        cartAdapter.notifyItemRemoved(position);
        updateTotalPrice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartListenerRegistration != null) {
            cartListenerRegistration.remove();
        }
    }
}