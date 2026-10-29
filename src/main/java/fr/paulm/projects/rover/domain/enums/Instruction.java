package fr.paulm.projects.rover.domain.enums;

// The commands a rover can execute. How they are written in an input (for example L, R and M in the text format)
// is the input format's concern, not the domain's.
public enum Instruction {
    LEFT, RIGHT, MOVE
}
