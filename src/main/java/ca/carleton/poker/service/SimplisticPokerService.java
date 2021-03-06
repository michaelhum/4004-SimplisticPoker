package ca.carleton.poker.service;

import ca.carleton.poker.entity.PokerHand;
import ca.carleton.poker.entity.card.Card;
import ca.carleton.poker.entity.card.Rank;
import ca.carleton.poker.entity.card.Suit;
import ca.carleton.poker.service.rank.PokerRankService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import static java.lang.Integer.compare;
import static java.util.Collections.reverse;
import static java.util.Collections.sort;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Methods for the capture and determination of poker hand ranks.
 * <p>
 * Created by Mike on 14/09/2015.
 */
public final class SimplisticPokerService {

    private final PokerRankService pokerRankService;

    private static final List<String> roundCardCache = new ArrayList<>();

    private static final List<String> roundPlayerIdCache = new ArrayList<>();

    /**
     * Constructor dependency injection...why do I not have spring?
     *
     * @param pokerRankService the poker rank service.
     */
    public SimplisticPokerService(final PokerRankService pokerRankService) {
        this.pokerRankService = pokerRankService;
    }

    /**
     * Clear the caches.
     */
    public void clearCaches() {
        roundCardCache.clear();
        roundPlayerIdCache.clear();
    }

    private static void checkPlayerIdExists(final String playerId) {
        if (roundPlayerIdCache.contains(playerId)) {
            throw new IllegalArgumentException("player id already in use");
        } else {
            roundPlayerIdCache.add(playerId);
        }
    }

    private static void checkCardExists(final String card) {
        if (roundCardCache.contains(card)) {
            throw new IllegalArgumentException(String.format("token %s is invalid (possible duplicate card, etc.)",
                    card));
        } else {
            roundCardCache.add(card);
        }
    }

    /**
     * Create a new poker hand from the given string input.
     * <p>
     * This method creates cards in the order they were input.
     * <p>
     * Expected input is of the form "PlayerId RankSuit RankSuit RankSuit RankSuit RankSuit"
     *
     * @param input the input.
     * @return the poker hand.
     * @throws java.lang.IllegalArgumentException if an invalid string is passed in.
     */
    public PokerHand makeHand(final String input) throws IllegalArgumentException {

        if (isEmpty(input)) {
            throw new IllegalArgumentException("input may not be null");
        }

        final StringTokenizer tokens = new StringTokenizer(input, " ");

        if (tokens.countTokens() != 6) {
            throw new IllegalArgumentException("input requires 6 space-delimited tokens");
        }

        final String firstToken = tokens.nextToken();
        final int playerId;
        checkPlayerIdExists(firstToken);
        try {
            playerId = Integer.parseInt(firstToken);
        } catch (final NumberFormatException exception) {
            throw new IllegalArgumentException("first token must be an integer player id");
        }

        final PokerHand pokerHand = new PokerHand();

        while (tokens.hasMoreTokens()) {

            final String token = tokens.nextToken();

            // Check for existing tokens already used
            checkCardExists(token);
            String tokenToCompare = token.toLowerCase();

            // First, find the rank of the input, if any.
            Rank tokenRank = null;
            for (final Rank rank : Rank.values()) {
                final String rankValue = rank.toString().toLowerCase();
                if (tokenToCompare.startsWith(rankValue)) {
                    tokenRank = rank;
                    tokenToCompare = tokenToCompare.replace(rankValue, "");
                    break;
                }
            }
            if (tokenRank == null) {
                throw new IllegalArgumentException("invalid token " + token);
            }

            // Next, find the suit of the input, if any.
            Suit tokenSuit = null;
            for (final Suit suit : Suit.values()) {
                final String suitValue = suit.toString().toLowerCase();
                if (tokenToCompare.startsWith(suitValue)) {
                    tokenSuit = suit;
                    break;
                }
            }
            if (tokenSuit == null) {
                throw new IllegalArgumentException("invalid token " + token);
            }

            pokerHand.setPlayerId(playerId);
            pokerHand.addCard(new Card(tokenRank, tokenSuit));
        }
        return pokerHand;
    }

    /**
     * Give each poker hand a rank (five high, four of a kind, etc).
     *
     * @param pokerHands the list of hands.
     */
    public void assignPokerRanks(final List<PokerHand> pokerHands) {
        pokerHands.forEach(this.pokerRankService::rankHand);
    }

    /**
     * Sorts the given list of hands into their final ranks, in descending order.
     * This method also sets the finalRanking int field in the case of ties, etc.
     *
     * @param pokerHands the given hands.
     * @return the sorted hands with their rank set.
     */
    public List<PokerHand> sortAndSetFinalRankings(final List<PokerHand> pokerHands) {
        this.assignPokerRanks(pokerHands);
        final Comparator<PokerHand> pokerHandComparator = (hand1, hand2) -> hand1.getPokerRank()
                .compareTo(hand2.getPokerRank());

        sort(pokerHands, pokerHandComparator);
        reverse(pokerHands);

        int rank = 2;
        pokerHands.get(0).setFinalRank(1);
        for (int i = 1; i < pokerHands.size(); i++) {
            final PokerHand currentHand = pokerHands.get(i);
            // Need to check to see if equal hands...
            final PokerHand previousHand = pokerHands.get(i - 1);
            if (currentHand.equals(previousHand)) {
                currentHand.setFinalRank(previousHand.getFinalRank());
            } else {
                currentHand.setFinalRank(rank++);
            }
        }

        // Sort by final ranking and/or player id.
        final Comparator<PokerHand> finalComparator = (hand1, hand2) -> {
            final int rankResult = compare(hand1.getFinalRank(), (hand2.getFinalRank()));
            return rankResult == 0 ? compare(hand1.getPlayerId(), hand2.getPlayerId()) : rankResult;
        };

        sort(pokerHands, finalComparator);

        return pokerHands;
    }

}
