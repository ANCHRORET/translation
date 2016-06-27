package com.ancho.tv;

import java.util.ArrayList;
import java.util.List;

public final class MovieList {
    public static final String MOVIE_CATEGORY[] = {
            "Category Zero",
            "Category One",
            "Category Two",
            "Category Three",
            "Category Four",
            "Category Five",
    };

    public static List<Movie> list;

    public static List<Movie> setupMovies() {
        list = new ArrayList<Movie>();
        String title[] = {
                "Zeitgeist 2010_ Year in Review",
                "Google Demo Slam_ 20ft Search",
                "Introducing Gmail Blue",
                "Introducing Google Fiber to the Pole",
                "Introducing Google Nose"
        };

        String description = "Fusce id nisi turpis. Praesent viverra bibendum semper. "
                + "Donec tristique, orci sed semper lacinia, quam erat rhoncus massa, non congue tellus est "
                + "quis tellus. Sed mollis orci venenatis quam scelerisque accumsan. Curabitur a massa sit "
                + "amet mi accumsan mollis sed et magna. Vivamus sed aliquam risus. Nulla eget dolor in elit "
                + "facilisis mattis. Ut aliquet luctus lacus. Phasellus nec commodo erat. Praesent tempus id "
                + "lectus ac scelerisque. Maecenas pretium cursus lectus id volutpat.";

        String videoUrl[] = {
                "http://s.bemetoy.com/dance/bd/bd494888fc3d058488c448d10e93a7d8.mp4",
                "http://s.bemetoy.com/dance/bd/bd494888fc3d058488c448d10e93a7d8.mp4",
                "http://s.bemetoy.com/dance/bd/bd494888fc3d058488c448d10e93a7d8.mp4",
                "http://s.bemetoy.com/dance/bd/bd494888fc3d058488c448d10e93a7d8.mp4",
                "http://s.bemetoy.com/dance/bd/bd494888fc3d058488c448d10e93a7d8.mp4"
        };
        String bgImageUrl[] = {
                "drawable://"+R.drawable.bm_global_bg,
                "drawable://"+R.drawable.bm_global_bg,
                "drawable://"+R.drawable.bm_global_bg,
                "drawable://"+R.drawable.bm_global_bg,
                "drawable://"+R.drawable.bm_global_bg,
        };
        String cardImageUrl[] = {
                "http://s.bemetoy.com/img/0d/0d2f581494cccfd310760ea98dd344b0.png",
                "http://s.bemetoy.com/img/c8/c82d575937b5c6ffbbcd8efa3d0657df.png",
                "http://s.bemetoy.com/img/22/22494560fe87faab91ef70d2abdebf39.png",
                "http://s.bemetoy.com/img/c8/c82d575937b5c6ffbbcd8efa3d0657df.png",
                "http://s.bemetoy.com/img/22/22494560fe87faab91ef70d2abdebf39.png"
        };

        list.add(buildMovieInfo("主页", title[0],
                description, "Studio Zero", videoUrl[0], cardImageUrl[0], bgImageUrl[0]));
        list.add(buildMovieInfo("category", title[1],
                description, "Studio One", videoUrl[1], cardImageUrl[1], bgImageUrl[1]));
        list.add(buildMovieInfo("category", title[2],
                description, "Studio Two", videoUrl[2], cardImageUrl[2], bgImageUrl[2]));
        list.add(buildMovieInfo("category", title[3],
                description, "Studio Three", videoUrl[3], cardImageUrl[3], bgImageUrl[3]));
        list.add(buildMovieInfo("category", title[4],
                description, "Studio Four", videoUrl[4], cardImageUrl[4], bgImageUrl[4]));

        return list;
    }

    private static Movie buildMovieInfo(String category, String title,
            String description, String studio, String videoUrl, String cardImageUrl,
            String bgImageUrl) {
        Movie movie = new Movie();
        movie.setId(Movie.getCount());
        Movie.incCount();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setStudio(studio);
        movie.setCategory(category);
        movie.setCardImageUrl(cardImageUrl);
        movie.setBackgroundImageUrl(bgImageUrl);
        movie.setVideoUrl(videoUrl);
        return movie;
    }
}
