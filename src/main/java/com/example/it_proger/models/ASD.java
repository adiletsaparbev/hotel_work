package com.example.it_proger.models;

public class ASD {

    int s;
    int d;
    /**
     * Create an ASD instance with the specified s and d values.
     *
     * @param s the value to assign to the instance's s field
     * @param d the value to assign to the instance's d field
     */
    ASD(int s, int d) {
        this.s = s;
        this.d = d;
    }
        /**
         * Retrieves the current value of s.
         *
         * @return the current value of s.
         */
        public int getS() {
            return s;
        }

        /**
         * Gets the current value of d.
         *
         * @return the value of d
         */
        public int getD() {
            return d;
        }

}
