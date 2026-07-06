package com.intelliguard.config;

import com.intelliguard.entity.AppUser;
import com.intelliguard.entity.Transaction;
import com.intelliguard.repository.TransactionRepository;
import com.intelliguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * DataSeeder runs once on startup and loads demo data
 * so the dashboard looks impressive from the first second.
 *
 * Creates:
 * - 3 default users (admin, analyst, manager)
 * - 20 realistic transactions (mix of APPROVE/REVIEW/BLOCK)
 *
 * Only runs if no transactions exist yet.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.demo.seed-data", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedTransactions();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;

        List<AppUser> users = List.of(
                AppUser.builder().username("admin").tenantId("demo-bank").password(passwordEncoder.encode("password123")).role("ADMIN").enabled(true).build(),
                AppUser.builder().username("analyst").tenantId("demo-bank").password(passwordEncoder.encode("analyst123")).role("ANALYST").enabled(true).build(),
                AppUser.builder().username("manager").tenantId("demo-bank").password(passwordEncoder.encode("manager123")).role("MANAGER").enabled(true).build()
        );
        userRepository.saveAll(users);
        log.info("✅ Demo users created: admin/password123, analyst/analyst123, manager/manager123");
    }

    private void seedTransactions() {
        if (transactionRepository.count() > 0) return;

        List<Transaction> transactions = List.of(
                // Normal approved transactions
                buildTxn("RAHUL_KUMAR", "VENDOR_001", "5000", "IN", "UPI", "MOBILE", "APPROVE", "0.0420", null),
                buildTxn("PRIYA_SHARMA", "AMAZON_IN", "2499", "IN", "CARD", "DESKTOP", "APPROVE", "0.0312", null),
                buildTxn("MOHAMMED_ALI", "ZOMATO", "850", "IN", "UPI", "MOBILE", "APPROVE", "0.0180", null),
                buildTxn("ANITA_PATEL", "FLIPKART", "12999", "IN", "CARD", "DESKTOP", "APPROVE", "0.0540", null),
                buildTxn("VIKRAM_SINGH", "VENDOR_002", "3200", "IN", "NET_BANKING", "DESKTOP", "APPROVE", "0.0290", null),
                buildTxn("SUNITA_REDDY", "SWIGGY", "450", "IN", "UPI", "MOBILE", "APPROVE", "0.0150", null),
                buildTxn("ARJUN_NAIR", "VENDOR_003", "8500", "IN", "UPI", "MOBILE", "APPROVE", "0.0610", null),
                buildTxn("DEEPA_IYER", "NETFLIX_IN", "649", "IN", "CARD", "DESKTOP", "APPROVE", "0.0220", null),

                // Review transactions
                buildTxn("RAJESH_GUPTA", "VENDOR_004", "520000", "IN", "NET_BANKING", "DESKTOP", "REVIEW", "0.4500",
                        "AmountThresholdRule: Amount ₹520000 exceeds ₹5,00,000"),
                buildTxn("KAVITA_JOSHI", "FOREIGN_CO", "85000", "VN", "CARD", "UNKNOWN", "REVIEW", "0.5200",
                        "CountryBlocklistRule: Elevated-risk country: VN | UnknownDeviceRule"),
                buildTxn("SURESH_KUMAR", "VENDOR_005", "750000", "IN", "NET_BANKING", "DESKTOP", "REVIEW", "0.4800",
                        "AmountThresholdRule: Amount ₹750000 exceeds ₹5,00,000"),

                // Blocked transactions
                buildTxn("UNKNOWN_001", "RECEIVER_X", "98000", "NG", "NET_BANKING", "UNKNOWN", "BLOCK", "0.9500",
                        "CountryBlocklistRule: Transaction from FATF high-risk country: NG"),
                buildTxn("HACKER_001", "RECEIVER_Y", "45000", "KP", "UPI", "UNKNOWN", "BLOCK", "0.9800",
                        "CountryBlocklistRule: Transaction from FATF high-risk country: KP"),
                buildTxn("FRAUD_RING_A", "MULE_001", "1500000", "IN", "NET_BANKING", "DESKTOP", "BLOCK", "0.8000",
                        "AmountThresholdRule: Amount ₹1500000 exceeds maximum limit of ₹10,00,000"),
                buildTxn("FRAUD_RING_B", "MULE_002", "1200000", "NG", "NET_BANKING", "UNKNOWN", "BLOCK", "1.0000",
                        "CountryBlocklistRule: NG | AmountThresholdRule: Exceeds 10L | UnknownDeviceRule"),
                buildTxn("BOT_ATTACKER", "RECEIVER_Z", "9900", "IR", "UPI", "UNKNOWN", "BLOCK", "0.9700",
                        "CountryBlocklistRule: Transaction from FATF high-risk country: IR"),
                buildTxn("STOLEN_CARD", "ONLINE_SHOP", "87500", "IN", "CARD", "UNKNOWN", "BLOCK", "0.7800",
                        "VelocityRule: 15 transactions in 10 minutes | UnknownDeviceRule"),
                buildTxn("ACCOUNT_TAKEOVER", "WIRE_OUT", "430000", "SY", "NET_BANKING", "DESKTOP", "BLOCK", "0.9600",
                        "CountryBlocklistRule: Transaction from FATF high-risk country: SY"),
                buildTxn("MONEY_LAUNDERER", "SHELL_CO", "990000", "AF", "NET_BANKING", "DESKTOP", "BLOCK", "0.9900",
                        "CountryBlocklistRule: AF | AmountThresholdRule: Exceeds 5L"),
                buildTxn("CARDING_BOT", "MERCHANT_X", "4999", "IN", "CARD", "UNKNOWN", "BLOCK", "0.7500",
                        "VelocityRule: Velocity spike detected | UnknownDeviceRule")
        );

        transactionRepository.saveAll(transactions);
        log.info("✅ {} demo transactions seeded successfully", transactions.size());
    }

    private Transaction buildTxn(String sender, String receiver, String amount,
                                 String country, String method, String device,
                                 String status, String score, String reason) {
        return Transaction.builder()
                .senderId(sender)
                .tenantId("demo-bank")
                .receiverId(receiver)
                .amount(new BigDecimal(amount))
                .currency("INR")
                .country(country)
                .paymentMethod(method)
                .deviceType(device)
                .ipAddress("192.168.1.1")
                .status(status)
                .fraudScore(new BigDecimal(score))
                .flagReason(reason)
                .build();
    }
}
