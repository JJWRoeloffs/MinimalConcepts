package edu.vuamsterdam.MinimalConcepts;

// I could use apache commons, but I can also just do this.
public record Pair<T1, T2>(T1 first, T2 second) {}